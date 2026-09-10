package cn.mine.minestars.ui.pages.assistant

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.dao.PresetDAO
import cn.mine.minestars.data.db.dao.PresetEntryDAO
import cn.mine.minestars.data.db.dao.TagDAO
import cn.mine.minestars.data.db.toEntity
import cn.mine.minestars.data.db.toModel
import cn.mine.minestars.data.export.CharacterCardExporter
import cn.mine.minestars.data.files.FilesManager
import cn.mine.minestars.data.import.DefaultPresetManager
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.data.model.Avatar
import cn.mine.minestars.data.model.Tag
import cn.mine.minestars.data.repository.ConversationRepository
import cn.mine.minestars.data.repository.MemoryRepository
import kotlin.uuid.Uuid

class AssistantVM(
    private val settingsStore: SettingsStore,
    private val memoryRepository: MemoryRepository,
    private val conversationRepo: ConversationRepository,
    private val filesManager: FilesManager,
    private val presetDao: PresetDAO,
    private val presetEntryDao: PresetEntryDAO,
    private val assistantDao: AssistantDAO,
    private val tagDao: TagDAO,
) : ViewModel() {
    val settings: StateFlow<Settings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings.dummy())

    val assistants: StateFlow<List<Assistant>> = assistantDao.getAllFlow()
        .map { entities -> entities.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tags: StateFlow<List<Tag>> = tagDao.getAllFlow()
        .map { entities -> entities.map { Tag(id = kotlin.uuid.Uuid.parse(it.id), name = it.name) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _events = MutableSharedFlow<Result<String>>()
    val events = _events.asSharedFlow()

    fun reorderAssistants(from: Int, to: Int, allAssistants: List<Assistant>) {
        viewModelScope.launch {
            val reordered = allAssistants.toMutableList().apply {
                add(to, removeAt(from))
            }
            reordered.forEachIndexed { index, assistant ->
                assistantDao.insert(assistant.toEntity().copy(displayOrder = index))
            }
        }
    }

    fun reorderTags(from: Int, to: Int, allTags: List<Tag>) {
        viewModelScope.launch {
            val reordered = allTags.toMutableList().apply {
                add(to, removeAt(from))
            }
            // Tags don't have display_order, so we just save the order
            tagDao.insertAll(reordered.map { cn.mine.minestars.data.db.entity.TagEntity(id = it.id.toString(), name = it.name) })
        }
    }

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsStore.update(settings)
        }
    }

    fun addAssistant(assistant: Assistant) {
        viewModelScope.launch {
            // Ensure the default preset exists before binding
            DefaultPresetManager.ensureDefaultPresetExists(presetDao, presetEntryDao)

            val finalAssistant = if (assistant.presetId == null) {
                val defaultPreset = presetDao.getDefaultPreset()
                if (defaultPreset != null) {
                    val defaultUuid = runCatching { Uuid.parse(defaultPreset.id) }.getOrNull()
                    if (defaultUuid != null) assistant.copy(presetId = defaultUuid) else assistant
                } else {
                    assistant
                }
            } else {
                assistant
            }
            assistantDao.insert(finalAssistant.toEntity())
        }
    }

    fun removeAssistant(assistant: Assistant) {
        viewModelScope.launch {
            cleanupAssistantFiles(assistant)

            assistantDao.deleteById(assistant.id.toString())
            memoryRepository.deleteMemoriesOfAssistant(assistant.id.toString())
            conversationRepo.deleteConversationOfAssistant(assistant.id)
        }
    }

    private fun cleanupAssistantFiles(assistant: Assistant) {
        val uris = buildList {
            (assistant.avatar as? Avatar.Image)?.let { add(it.url.toUri()) }
            assistant.background?.let { add(it.toUri()) }
        }

        if (uris.isNotEmpty()) {
            filesManager.deleteChatFiles(uris)
        }
    }

    fun copyAssistant(assistant: Assistant) {
        viewModelScope.launch {
            val copiedAssistant = assistant.copy(
                id = kotlin.uuid.Uuid.random(),
                name = "${assistant.name} (Clone)",
                charName = "${assistant.charName} (Clone)",
                avatar = if(assistant.avatar is Avatar.Image) Avatar.Dummy else assistant.avatar,
            )
            assistantDao.insert(copiedAssistant.toEntity())
        }
    }

    fun getMemories(assistant: Assistant) =
        if (assistant.useGlobalMemory) {
            memoryRepository.getGlobalMemoriesFlow()
        } else {
            memoryRepository.getMemoriesOfAssistantFlow(assistant.id.toString())
        }

    fun exportCharacterCard(
        assistant: Assistant,
        options: ExportOptions,
        resolver: ContentResolver,
        uri: Uri,
    ) {
        viewModelScope.launch {
            runCatching {
                val specVersion = if (options.specVersion == SpecVersion.V3) "3.0" else "2.0"
                val json = CharacterCardExporter.buildCharacterCardJson(
                    assistant = assistant,
                    specVersion = specVersion,
                    includeWorldBook = options.includeWorldBook,
                    includeRegex = options.includeRegex,
                )

                val bytes = if (options.format == ExportFormat.PNG) {
                    val bitmap = withContext(Dispatchers.IO) {
                        (assistant.avatar as? Avatar.Image)?.let { avatar ->
                            resolver.openInputStream(Uri.parse(avatar.url))?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        }
                    } ?: throw Exception("PNG export requires an image avatar")
                    CharacterCardExporter.encodeAsPng(
                        avatarBitmap = bitmap,
                        cardJson = json,
                    )
                } else {
                    json.toString(4).toByteArray(Charsets.UTF_8)
                }

                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                } ?: throw Exception("无法打开文件")
            }.onSuccess {
                _events.emit(Result.success("角色卡已导出"))
            }.onFailure {
                _events.emit(Result.failure(it))
            }
        }
    }
}
