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
import org.json.JSONObject


sealed interface AssistantWorldBookListEvent {
    data class Success(val message: String) : AssistantWorldBookListEvent
    data class Error(val message: String) : AssistantWorldBookListEvent
}

class AssistantWorldBookListVM(
    private val assistantDao: AssistantDAO,
) : ViewModel() {
    private val _assistant = MutableStateFlow<Assistant?>(null)
    val assistant = _assistant.asStateFlow()

    private val _events = MutableSharedFlow<AssistantWorldBookListEvent>()
    val events = _events.asSharedFlow()

    fun loadAssistant(assistantId: String) {
        viewModelScope.launch {
            val entity = assistantDao.getById(assistantId) ?: return@launch
            _assistant.value = entity.toModel()
        }
    }

    /** Whether this assistant has a world book. */
    fun hasWorldBook(): Boolean {
        return !_assistant.value?.worldBookJson.isNullOrBlank()
    }

    /** The world book JSON string, or null. */
    fun getWorldBookJson(): String? {
        val json = _assistant.value?.worldBookJson
        if (json.isNullOrBlank()) return null
        return json
    }

    /** Get world book name from JSON. */
    fun getWorldBookName(): String {
        return _assistant.value?.worldBookJson?.let { json ->
            runCatching { JSONObject(json).optString("name", "世界书") }.getOrNull()
        } ?: "世界书"
    }

    /** Whether the world book is enabled. */
    fun isWorldBookEnabled(): Boolean {
        return _assistant.value?.worldBookJson?.let { json ->
            runCatching { JSONObject(json).optBoolean("enabled", true) }.getOrNull()
        } ?: true
    }

    /** Toggle world book enabled flag. */
    fun toggleWorldBookEnabled() {
        val current = _assistant.value ?: return
        val json = current.worldBookJson ?: return
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return
        obj.put("enabled", !obj.optBoolean("enabled", true))
        updateWorldBookJson(current, obj.toString())
    }

    /** Set enabled on all world books (just the one). */
    fun toggleAllWorldBooks(enabled: Boolean) {
        val current = _assistant.value ?: return
        val json = current.worldBookJson ?: return
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return
        obj.put("enabled", enabled)
        updateWorldBookJson(current, obj.toString())
    }

    /** Ensure a world book exists, return its JSON. */
    suspend fun ensureWorldBook(): String {
        val current = _assistant.value ?: return "{}"
        val existing = current.worldBookJson
        if (!existing.isNullOrBlank()) return existing

        val defaultJson = """{"name":"世界书","entries":[],"enabled":true}"""
        updateWorldBookJson(current, defaultJson)
        return defaultJson
    }

    /** Delete the world book (set to null). */
    fun deleteWorldBook() {
        val current = _assistant.value ?: return
        updateWorldBookJson(current, null)
    }

    private fun updateWorldBookJson(assistant: Assistant, json: String?) {
        _assistant.value = assistant.copy(worldBookJson = json)
        viewModelScope.launch {
            val entity = assistantDao.getById(assistant.id.toString()) ?: return@launch
            assistantDao.update(entity.copy(worldBookJson = json))
        }
    }
}
