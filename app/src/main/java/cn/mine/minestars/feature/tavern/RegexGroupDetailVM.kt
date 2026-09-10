package cn.mine.minestars.feature.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import cn.mine.minestars.data.db.dao.RegexGroupDAO
import cn.mine.minestars.data.db.dao.RegexScriptDAO
import cn.mine.minestars.data.db.entity.RegexGroupEntity
import cn.mine.minestars.data.db.entity.RegexScriptEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface RegexGroupDetailEvent {
    data class Success(val message: String) : RegexGroupDetailEvent
    data class Error(val message: String) : RegexGroupDetailEvent
}

class RegexGroupDetailVM(
    private val regexGroupDao: RegexGroupDAO,
    private val regexScriptDao: RegexScriptDAO,
) : ViewModel() {
    private val _group = MutableStateFlow<RegexGroupEntity?>(null)
    val group = _group.asStateFlow()

    private val _scripts = MutableStateFlow<List<RegexScriptEntity>>(emptyList())
    val scripts = _scripts.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges = _hasUnsavedChanges.asStateFlow()

    private val _events = MutableSharedFlow<RegexGroupDetailEvent>()
    val events = _events.asSharedFlow()

    /** IDs of scripts that existed at load time — used to compute deletes on save. */
    private var originalScriptIds: Set<String> = emptySet()

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            val g = regexGroupDao.getById(groupId) ?: return@launch
            _group.value = g
            reloadScripts(g)
            originalScriptIds = _scripts.value.map { it.id }.toSet()
            _hasUnsavedChanges.value = false
        }
    }

    /** Add a new script in memory only. */
    fun addScript() {
        val g = _group.value ?: return
        val id = UUID.randomUUID().toString()
        val script = RegexScriptEntity(
            id = id,
            name = "新建正则",
            rawJson = """{"scriptName":"新建正则","findRegex":"","replaceString":"","trimStrings":[],"placement":[1,2],"disabled":false,"markdownOnly":false,"promptOnly":false,"runOnEdit":true,"substituteRegex":0}""",
            scope = g.scope.ifBlank { "IMPORTED" },
            groupId = g.id,
        )
        _scripts.value = _scripts.value + script
        _group.value = g.copy(regexIdsJson = JSONArray(_scripts.value.map { it.id }).toString())
        _hasUnsavedChanges.value = true
    }

    /** Copy a script in memory only. */
    fun copyScript(scriptId: String) {
        val original = _scripts.value.find { it.id == scriptId } ?: return
        val newId = UUID.randomUUID().toString()
        val copy = original.copy(
            id = newId,
            name = "${original.name} (副本)",
        )
        _scripts.value = _scripts.value + copy
        _group.value = _group.value?.let { g ->
            g.copy(regexIdsJson = JSONArray(_scripts.value.map { it.id }).toString())
        }
        _hasUnsavedChanges.value = true
    }

    /** Remove a script in memory only. */
    fun deleteScript(scriptId: String) {
        _scripts.value = _scripts.value.filter { it.id != scriptId }
        _group.value = _group.value?.let { g ->
            g.copy(regexIdsJson = JSONArray(_scripts.value.map { it.id }).toString())
        }
        _hasUnsavedChanges.value = true
    }

    /** Reorder scripts in memory only. */
    fun reorderScripts(fromIndex: Int, toIndex: Int) {
        val list = _scripts.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val moved = list.removeAt(fromIndex)
        list.add(toIndex, moved)
        _scripts.value = list
        _group.value = _group.value?.let { g ->
            g.copy(regexIdsJson = JSONArray(list.map { it.id }).toString())
        }
        _hasUnsavedChanges.value = true
    }

    /** Update group name in memory only. */
    fun updateGroupName(name: String) {
        _group.value = _group.value?.copy(name = name)
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

    /** Update a script's rawJson in memory. Called by RegexScriptDetailPage. */
    fun updateScriptRawJson(scriptId: String, rawJson: String) {
        _scripts.value = _scripts.value.map {
            if (it.id == scriptId) it.copy(rawJson = rawJson) else it
        }
        _hasUnsavedChanges.value = true
    }

    /** Write all in-memory changes to DB at once. */
    suspend fun save() {
        try {
            val g = _group.value ?: return

            // Delete scripts that were removed
            val currentIds = _scripts.value.map { it.id }.toSet()
            val deletedIds = originalScriptIds - currentIds
            for (id in deletedIds) {
                regexScriptDao.deleteById(id)
            }

            // Insert/update all current scripts
            for (script in _scripts.value) {
                regexScriptDao.insert(script)
            }

            // Update group
            val newIdsJson = JSONArray(_scripts.value.map { it.id }).toString()
            regexGroupDao.update(g.copy(regexIdsJson = newIdsJson))

            _group.value = g.copy(regexIdsJson = newIdsJson)
            originalScriptIds = currentIds
            _hasUnsavedChanges.value = false
            _events.emit(RegexGroupDetailEvent.Success("已保存"))
        } catch (e: Exception) {
            _events.emit(RegexGroupDetailEvent.Error("保存失败: ${e.message}"))
        }
    }

    private suspend fun reloadScripts(g: RegexGroupEntity) {
        val allScripts = regexScriptDao.getAll()
        val scriptIds = parseIds(g.regexIdsJson)
        _scripts.value = scriptIds.mapNotNull { id -> allScripts.find { it.id == id } }
    }

    private fun parseIds(json: String): List<String> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
