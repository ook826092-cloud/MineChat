package cn.mine.minestars.feature.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.data.db.dao.PresetDAO
import cn.mine.minestars.data.db.dao.PresetEntryDAO
import cn.mine.minestars.data.db.entity.PresetEntryEntity
import cn.mine.minestars.data.import.CharacterImportService
import cn.mine.minestars.data.import.DefaultPresetManager
import cn.mine.minestars.core.tavern.preset.DefaultPresetFactory
import cn.mine.minestars.core.tavern.preset.PresetParser
import java.util.UUID

sealed interface PresetListEvent {
    data class Success(val message: String) : PresetListEvent
    data class Error(val message: String) : PresetListEvent
}

class PresetListVM(
    private val presetDao: PresetDAO,
    private val presetEntryDao: PresetEntryDAO,
) : ViewModel() {
    val presets = presetDao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _events = MutableSharedFlow<PresetListEvent>()
    val events = _events.asSharedFlow()

    private val _importing = MutableStateFlow(false)
    val importing = _importing.asStateFlow()

    init {
        viewModelScope.launch {
            DefaultPresetManager.ensureDefaultPresetExists(presetDao, presetEntryDao)
        }
    }

    fun createPreset(name: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val presetName = name.ifBlank { "新建预设" }
            val rawJson = DefaultPresetFactory.createDefaultPresetJson().let { json ->
                org.json.JSONObject(json).put("name", presetName).toString()
            }
            val presetObj = org.json.JSONObject(rawJson)
            val entries = PresetParser.extractEntries(presetObj)
            val entryEntities = entries.mapIndexed { index, entry ->
                PresetEntryEntity(
                    presetId = id,
                    entryIndex = index,
                    id = entry.id,
                    identifier = entry.identifier,
                    name = entry.name,
                    enabled = entry.enabled,
                    role = entry.role,
                    content = entry.content,
                    injectionPosition = entry.injectionPosition,
                    injectionDepth = entry.injectionDepth,
                    injectionOrder = entry.injectionOrder,
                    systemPrompt = entry.systemPrompt,
                    marker = entry.marker,
                    forbidOverrides = entry.forbidOverrides,
                    injectionTriggerJson = org.json.JSONArray(entry.injectionTrigger).toString(),
                    mounted = entry.mounted,
                )
            }
            presetDao.insert(
                cn.mine.minestars.data.db.entity.PresetEntity(
                    id = id,
                    name = presetName,
                    rawJson = rawJson,
                    isDefault = false,
                )
            )
            if (entryEntities.isNotEmpty()) {
                presetEntryDao.insertAll(entryEntities)
            }
            _events.emit(PresetListEvent.Success("预设已创建"))
        }
    }

    fun importPreset(resolver: android.content.ContentResolver, uri: android.net.Uri) {
        viewModelScope.launch {
            _importing.value = true
            try {
                val count = CharacterImportService.importPresets(resolver, uri)
                _events.emit(PresetListEvent.Success("导入了 $count 个预设"))
            } catch (e: Exception) {
                android.util.Log.e("PresetImport", "导入预设失败", e)
                _events.emit(PresetListEvent.Error("导入失败: ${e.message}"))
            } finally {
                _importing.value = false
            }
        }
    }

    fun copyPreset(id: String) {
        viewModelScope.launch {
            val preset = presetDao.getById(id) ?: return@launch
            val newId = UUID.randomUUID().toString()
            val newName = "${preset.name} (副本)"
            val newJson = org.json.JSONObject(preset.rawJson).apply {
                put("name", newName)
            }.toString()
            val entries = presetEntryDao.getByPresetId(id)
            presetDao.insert(preset.copy(
                id = newId,
                name = newName,
                rawJson = newJson,
                isDefault = false,
            ))
            if (entries.isNotEmpty()) {
                presetEntryDao.insertAll(entries.map { it.copy(presetId = newId) })
            }
            _events.emit(PresetListEvent.Success("预设已复制"))
        }
    }

    fun exportPreset(id: String, resolver: android.content.ContentResolver, uri: android.net.Uri) {
        viewModelScope.launch {
            runCatching {
                val preset = presetDao.getById(id) ?: throw Exception("预设不存在")
                val formatted = org.json.JSONObject(preset.rawJson).toString(4)
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(formatted.toByteArray(Charsets.UTF_8))
                } ?: throw Exception("无法打开文件")
            }.onSuccess {
                _events.emit(PresetListEvent.Success("预设已导出"))
            }.onFailure {
                _events.emit(PresetListEvent.Error("导出失败: ${it.message}"))
            }
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch {
            val preset = presetDao.getById(id)
            if (preset != null && preset.isDefault) {
                _events.emit(PresetListEvent.Error("默认预设不可删除"))
                return@launch
            }
            presetDao.deleteById(id)
        }
    }
}
