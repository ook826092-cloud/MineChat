package cn.mine.minestars.ui.pages.assistant.detail

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.dao.KnowledgeBaseDAO
import cn.mine.minestars.data.db.dao.McpServerDAO
import cn.mine.minestars.data.db.dao.ProviderDAO
import cn.mine.minestars.data.db.dao.TagDAO
import cn.mine.minestars.data.ai.mcp.toConfig
import cn.mine.minestars.data.db.toEntity
import cn.mine.minestars.data.db.toModel
import cn.mine.minestars.data.db.toProviderSettings
import cn.mine.minestars.data.files.FilesManager
import cn.mine.minestars.rag.KnowledgeBase
import cn.mine.minestars.data.files.SkillManager
import cn.mine.minestars.data.files.SkillMetadata
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.data.model.AssistantMemory
import cn.mine.minestars.data.model.Avatar
import cn.mine.minestars.data.model.Tag
import cn.mine.minestars.data.repository.MemoryRepository
import kotlin.uuid.Uuid

private const val TAG = "AssistantDetailVM"

class AssistantDetailVM(
    private val id: String,
    private val settingsStore: SettingsStore,
    private val memoryRepository: MemoryRepository,
    private val filesManager: FilesManager,
    private val skillManager: SkillManager,
    private val assistantDao: AssistantDAO,
    private val mcpServerDao: McpServerDAO,
    private val knowledgeBaseDao: KnowledgeBaseDAO,
    private val providerDao: ProviderDAO,
    private val tagDao: TagDAO,
) : ViewModel() {
    private val assistantId = Uuid.parse(id)

    private val _skills = MutableStateFlow<List<SkillMetadata>>(emptyList())
    val skills = _skills.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _skills.value = skillManager.listSkills()
        }
    }

    val settings: StateFlow<Settings> =
        settingsStore.settingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, Settings.dummy())

    val mcpServerConfigs = mcpServerDao
        .getAllFlow().map { entities ->
            entities.map { it.toConfig() }
        }.stateIn(
            scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
        )

    val knowledgeBases = knowledgeBaseDao
        .getAllFlow().map { entities ->
            entities.map { entity ->
                KnowledgeBase(
                    id = kotlin.uuid.Uuid.parse(entity.id),
                    name = entity.name,
                    embeddingModelId = entity.embeddingModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() },
                    rerankModelId = entity.rerankModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() },
                    topK = entity.topK,
                    similarityThreshold = entity.similarityThreshold,
                    embeddingDimensions = entity.embeddingDimensions,
                )
            }
        }.stateIn(
            scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
        )

    val assistant: StateFlow<Assistant> = assistantDao
        .getByIdFlow(id)
        .map { entity ->
            entity?.toModel() ?: Assistant()
        }.stateIn(
            scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = Assistant()
        )

    val memories = assistant
        .flatMapLatest { currentAssistant ->
            if (currentAssistant.useGlobalMemory) {
                memoryRepository.getGlobalMemoriesFlow()
            } else {
                memoryRepository.getMemoriesOfAssistantFlow(assistantId.toString())
            }
        }
        .stateIn(
            scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
        )

    val providers = providerDao
        .getAllFlow()
        .map { it.toProviderSettings() }
        .stateIn(
            scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
        )

    val tags = tagDao
        .getAllFlow()
        .map { entities ->
            entities.map { Tag(id = kotlin.uuid.Uuid.parse(it.id), name = it.name) }
        }.stateIn(
            scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList()
        )

    fun updateTags(tagIds: List<Uuid>, tags: List<Tag>) {
        viewModelScope.launch {
            tagDao.insertAll(tags.map { cn.mine.minestars.data.db.entity.TagEntity(id = it.id.toString(), name = it.name) })
            val currentAssistant = assistant.value
            update(
                currentAssistant.copy(
                    tags = tagIds
                )
            )
            Log.d(TAG, "updateTags: ${tagIds.joinToString(",")}")
            cleanupUnusedTags()
        }
    }

    fun cleanupUnusedTags() {
        viewModelScope.launch {
            val allTags = tagDao.getAll()
            val validTagIds = allTags.map { kotlin.uuid.Uuid.parse(it.id) }.toSet()

            // 清理 assistant 中的无效 tag id
            val allAssistants = assistantDao.getAll().map { it.toModel() }
            for (assistant in allAssistants) {
                val validTags = assistant.tags.filter { tagId ->
                    validTagIds.contains(tagId)
                }
                if (validTags.size != assistant.tags.size) {
                    assistantDao.insert(assistant.copy(tags = validTags).toEntity())
                }
            }

            // 获取清理后的 assistant 中使用的 tag id
            val updatedAssistants = assistantDao.getAll().map { it.toModel() }
            val usedTagIds = updatedAssistants.flatMap { it.tags }.toSet()

            // 清理未使用的 tags
            val unusedTags = allTags.filter { tag ->
                kotlin.uuid.Uuid.parse(tag.id) !in usedTagIds
            }
            unusedTags.forEach { tag ->
                tagDao.deleteById(tag.id)
            }
        }
    }

    fun update(assistant: Assistant) {
        viewModelScope.launch {
            val currentEntity = assistantDao.getById(id)
            if (currentEntity != null) {
                val currentModel = currentEntity.toModel()
                checkAvatarDelete(old = currentModel, new = assistant)
                checkBackgroundDelete(old = currentModel, new = assistant)
            }
            assistantDao.update(assistant.toEntity())
        }
    }

    fun addMemory(memory: AssistantMemory) {
        viewModelScope.launch {
            val memoryAssistantId = if (assistant.value.useGlobalMemory) {
                MemoryRepository.GLOBAL_MEMORY_ID
            } else {
                assistantId.toString()
            }
            memoryRepository.addMemory(
                assistantId = memoryAssistantId,
                content = memory.content
            )
        }
    }

    fun updateMemory(memory: AssistantMemory) {
        viewModelScope.launch {
            memoryRepository.updateContent(id = memory.id, content = memory.content)
        }
    }

    fun deleteMemory(memory: AssistantMemory) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id = memory.id)
        }
    }

    fun checkAvatarDelete(old: Assistant, new: Assistant) {
        if (old.avatar is Avatar.Image && old.avatar != new.avatar) {
            filesManager.deleteChatFiles(listOf(old.avatar.url.toUri()))
        }
    }

    fun checkBackgroundDelete(old: Assistant, new: Assistant) {
        val oldBackground = old.background
        val newBackground = new.background

        if (oldBackground != null && oldBackground != newBackground) {
            try {
                val oldUri = oldBackground.toUri()
                if (oldUri.scheme == "content" || oldUri.scheme == "file") {
                    filesManager.deleteChatFiles(listOf(oldUri))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete background file: $oldBackground", e)
            }
        }
    }
}
