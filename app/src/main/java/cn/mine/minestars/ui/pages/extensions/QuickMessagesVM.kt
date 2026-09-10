package cn.mine.minestars.ui.pages.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.dao.QuickMessageDAO
import cn.mine.minestars.data.db.entity.QuickMessageEntity
import cn.mine.minestars.data.model.QuickMessage
import cn.mine.minestars.utils.JsonInstant
import kotlin.uuid.Uuid

class QuickMessagesVM(
    private val settingsStore: SettingsStore,
    private val assistantDao: AssistantDAO,
    private val quickMessageDao: QuickMessageDAO,
) : ViewModel() {
    val settings = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings.dummy())

    val quickMessages = quickMessageDao.getAllFlow()
        .map { entities ->
            entities.map {
                QuickMessage(
                    id = kotlin.uuid.Uuid.parse(it.id),
                    title = it.title,
                    content = it.content,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addQuickMessage(title: String, content: String) {
        viewModelScope.launch {
            val maxOrder = quickMessageDao.getAll().maxOfOrNull { it.displayOrder } ?: -1
            quickMessageDao.insert(
                QuickMessageEntity(
                    id = Uuid.random().toString(),
                    title = title,
                    content = content,
                    displayOrder = maxOrder + 1,
                )
            )
        }
    }

    fun updateQuickMessage(updated: QuickMessage) {
        viewModelScope.launch {
            val existing = quickMessageDao.getById(updated.id.toString())
            quickMessageDao.insert(
                QuickMessageEntity(
                    id = updated.id.toString(),
                    title = updated.title,
                    content = updated.content,
                    displayOrder = existing?.displayOrder ?: 0,
                )
            )
        }
    }

    fun deleteQuickMessage(id: Uuid) {
        viewModelScope.launch {
            val idStr = id.toString()
            quickMessageDao.deleteById(idStr)
            // Clean up quickMessageIds references in all assistants
            assistantDao.getAll().forEach { entity ->
                val ids = runCatching {
                    JsonInstant.decodeFromString<Set<Uuid>>(entity.quickMessageIds)
                }.getOrDefault(emptySet())
                if (id in ids) {
                    val newIds = ids - id
                    assistantDao.insert(
                        entity.copy(
                            quickMessageIds = JsonInstant.encodeToString(newIds)
                        )
                    )
                }
            }
        }
    }
}
