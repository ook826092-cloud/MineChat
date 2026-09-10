package cn.mine.minestars.ui.pages.chat

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.R
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.model.Folder
import cn.mine.minestars.data.repository.ConversationRepository
import cn.mine.minestars.data.repository.FolderRepository
import cn.mine.minestars.utils.toLocalString
import java.time.LocalDate
import java.time.ZoneId
import kotlin.uuid.Uuid

class ChatDrawerVM(
    private val context: Application,
    settingsStore: SettingsStore,
    conversationRepo: ConversationRepository,
    private val folderRepo: FolderRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val assistantId = settingsStore.settingsFlow
        .map { it.assistantId }
        .distinctUntilChanged()

    val folders: StateFlow<List<Folder>> = assistantId
        .flatMapLatest { id -> folderRepo.getFoldersOfAssistant(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedFolderId: StateFlow<Uuid?> = savedStateHandle
        .getStateFlow("selectedFolderId", null as Uuid?)
        .also { flow ->
            // 切换助手时重置选中文件夹
            viewModelScope.launch {
                assistantId.collect {
                    savedStateHandle["selectedFolderId"] = null
                }
            }
        }

    val conversations: Flow<PagingData<ConversationListItem>> =
        combine(assistantId, selectedFolderId) { id, folderId -> id to folderId }
            .flatMapLatest { (assistantId, folderId) ->
                if (folderId != null) {
                    conversationRepo.getConversationsOfFolderPaging(folderId)
                } else {
                    conversationRepo.getConversationsOfAssistantPaging(assistantId)
                }
            }
            .map { pagingData ->
                pagingData
                    .map { ConversationListItem.Item(it) }
                    .insertSeparators { before, after ->
                        when {
                            before == null && after is ConversationListItem.Item -> {
                                if (after.conversation.isPinned) {
                                    ConversationListItem.PinnedHeader
                                } else {
                                    val afterDate = after.conversation.updateAt
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                    ConversationListItem.DateHeader(
                                        date = afterDate,
                                        label = getDateLabel(afterDate)
                                    )
                                }
                            }

                            before is ConversationListItem.Item && after is ConversationListItem.Item -> {
                                if (before.conversation.isPinned && !after.conversation.isPinned) {
                                    val afterDate = after.conversation.updateAt
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                    ConversationListItem.DateHeader(
                                        date = afterDate,
                                        label = getDateLabel(afterDate)
                                    )
                                } else if (!after.conversation.isPinned) {
                                    val beforeDate = before.conversation.updateAt
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                    val afterDate = after.conversation.updateAt
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()

                                    if (beforeDate != afterDate) {
                                        ConversationListItem.DateHeader(
                                            date = afterDate,
                                            label = getDateLabel(afterDate)
                                        )
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                            }

                            else -> null
                        }
                    }
            }
            .cachedIn(viewModelScope)

    val scrollIndex: Int get() = savedStateHandle["scrollIndex"] ?: 0
    val scrollOffset: Int get() = savedStateHandle["scrollOffset"] ?: 0

    fun saveScrollPosition(index: Int, offset: Int) {
        savedStateHandle["scrollIndex"] = index
        savedStateHandle["scrollOffset"] = offset
    }

    // ── 文件夹操作 ──

    fun selectFolder(folderId: Uuid?) {
        savedStateHandle["selectedFolderId"] = folderId
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val id = assistantId.first()
            folderRepo.createFolder(id, name)
        }
    }

    fun renameFolder(id: Uuid, name: String) {
        viewModelScope.launch {
            folderRepo.renameFolder(id, name)
        }
    }

    fun deleteFolder(id: Uuid): Boolean {
        // 检查文件夹下是否还有正在生成中的会话
        // 简化处理：直接删除
        viewModelScope.launch {
            folderRepo.deleteFolder(id)
        }
        return true
    }

    fun moveConversationToFolder(conversationId: Uuid, folderId: Uuid?) {
        viewModelScope.launch {
            folderRepo.moveConversationToFolder(conversationId, folderId)
        }
    }

    private fun getDateLabel(date: LocalDate): String {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        return when (date) {
            today -> context.getString(R.string.chat_page_today)
            yesterday -> context.getString(R.string.chat_page_yesterday)
            else -> date.toLocalString(date.year != today.year)
        }
    }
}
