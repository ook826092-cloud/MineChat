package cn.mine.minestars.feature.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.toModel
import cn.mine.minestars.data.model.Assistant
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ScriptData(
    val id: String,
    val name: String,
    val rawJson: String,
    val sortOrder: Int,
    val enabled: Boolean,
)

sealed interface AssistantRegexListEvent {
    data class Success(val message: String) : AssistantRegexListEvent
    data class Error(val message: String) : AssistantRegexListEvent
}

class AssistantRegexListVM(
    private val assistantDao: AssistantDAO,
) : ViewModel() {
    private val _assistant = MutableStateFlow<Assistant?>(null)
    val assistant = _assistant.asStateFlow()

    private val _enabled = MutableStateFlow(true)
    val enabled = _enabled.asStateFlow()

    private val _scripts = MutableStateFlow<List<ScriptData>>(emptyList())
    val scripts = _scripts.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges = _hasUnsavedChanges.asStateFlow()

    private val _events = MutableSharedFlow<AssistantRegexListEvent>()
    val events = _events.asSharedFlow()

    private var loadedAssistantId: String = ""

    fun loadScripts(assistantId: String) {
        if (assistantId == loadedAssistantId && _scripts.value.isNotEmpty()) return
        loadedAssistantId = assistantId
        viewModelScope.launch {
            val entity = assistantDao.getById(assistantId) ?: return@launch
            val model = entity.toModel()
            _assistant.value = model
            _scripts.value = parseScriptsFromJson(model.regexScriptsJson)
            _hasUnsavedChanges.value = false
        }
    }

    private fun parseScriptsFromJson(json: String?): List<ScriptData> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            // Try new format: { "enabled": true, "scripts": [...] }
            val trimmed = json.trim()
            val arr = if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                _enabled.value = root.optBoolean("enabled", true)
                root.optJSONArray("scripts")
            } else {
                // Old format: bare JSONArray
                _enabled.value = true
                JSONArray(trimmed)
            }
            if (arr == null) return@runCatching emptyList<ScriptData>()
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ScriptData(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    name = obj.optString("name", ""),
                    rawJson = obj.optString("rawJson", "{}"),
                    sortOrder = obj.optInt("sortOrder", 0),
                    enabled = obj.optBoolean("enabled", true),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeScriptsToJson(scripts: List<ScriptData>): String {
        val arr = JSONArray()
        for (s in scripts) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("rawJson", s.rawJson)
            obj.put("sortOrder", s.sortOrder)
            obj.put("enabled", s.enabled)
            arr.put(obj)
        }
        val root = JSONObject()
        root.put("enabled", _enabled.value)
        root.put("scripts", arr)
        return root.toString()
    }

    private suspend fun persistScripts(scripts: List<ScriptData>) {
        val assistantId = loadedAssistantId
        if (assistantId.isBlank()) return
        val json = serializeScriptsToJson(scripts)
        _assistant.value = _assistant.value?.copy(regexScriptsJson = json)
        val entity = assistantDao.getById(assistantId) ?: return
        assistantDao.update(entity.copy(regexScriptsJson = json))
    }

    /** Add a new script in memory only. */
    fun addScript() {
        val newId = UUID.randomUUID().toString()
        val currentScripts = _scripts.value
        val newOrder = currentScripts.size
        val script = ScriptData(
            id = newId,
            name = "新建正则",
            rawJson = """{"scriptName":"新建正则","findRegex":"","replaceString":"","trimStrings":[],"placement":[1,2],"disabled":false,"markdownOnly":false,"promptOnly":false,"runOnEdit":true,"substituteRegex":0}""",
            sortOrder = newOrder,
            enabled = true,
        )
        _scripts.value = currentScripts + script
        _hasUnsavedChanges.value = true
    }

    /** Copy a script in memory only. */
    fun copyScript(scriptId: String) {
        val original = _scripts.value.find { it.id == scriptId } ?: return
        val newId = UUID.randomUUID().toString()
        val copy = original.copy(
            id = newId,
            name = "${original.name} (副本)",
            sortOrder = _scripts.value.size,
        )
        _scripts.value = _scripts.value + copy
        _hasUnsavedChanges.value = true
    }

    /** Delete a script in memory only. */
    fun deleteScript(scriptId: String) {
        _scripts.value = _scripts.value.filter { it.id != scriptId }
        _hasUnsavedChanges.value = true
    }

    /** Reorder scripts in memory only. */
    fun reorderScripts(fromIndex: Int, toIndex: Int) {
        val list = _scripts.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val moved = list.removeAt(fromIndex)
        list.add(toIndex, moved)
        _scripts.value = list.mapIndexed { index, script -> script.copy(sortOrder = index) }
        _hasUnsavedChanges.value = true
    }

    /** Toggle the entity-level enabled flag. */
    fun toggleScriptEntityEnabled(id: String) {
        _scripts.value = _scripts.value.map {
            if (it.id == id) it.copy(enabled = !it.enabled) else it
        }
        _hasUnsavedChanges.value = true
    }

    /** Toggle master enabled flag (root-level switch). */
    fun toggleEnabled() {
        _enabled.value = !_enabled.value
        _hasUnsavedChanges.value = true
    }

    /** Toggle disabled flag inside rawJson for a script. */
    fun toggleScriptEnabled(id: String) {
        _scripts.value = _scripts.value.map { script ->
            if (script.id == id) {
                val json = runCatching { JSONObject(script.rawJson) }.getOrNull() ?: return@map script
                json.put("disabled", !json.optBoolean("disabled", false))
                script.copy(rawJson = json.toString())
            } else {
                script
            }
        }
        _hasUnsavedChanges.value = true
    }

    /** Update a script's rawJson in memory. Called by detail page. */
    fun updateScriptRawJson(scriptId: String, rawJson: String) {
        _scripts.value = _scripts.value.map {
            if (it.id == scriptId) {
                val updatedName = runCatching { JSONObject(rawJson).optString("scriptName", "") }.getOrElse { "" }
                it.copy(rawJson = rawJson, name = updatedName.ifBlank { it.name })
            } else it
        }
        _hasUnsavedChanges.value = true
    }

    /** Write all in-memory changes to DB at once. */
    suspend fun save() {
        try {
            persistScripts(_scripts.value)
            _hasUnsavedChanges.value = false
            _events.emit(AssistantRegexListEvent.Success("已保存"))
        } catch (e: Exception) {
            _events.emit(AssistantRegexListEvent.Error("保存失败: ${e.message}"))
        }
    }
}
