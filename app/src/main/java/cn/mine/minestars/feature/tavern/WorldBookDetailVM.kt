package cn.mine.minestars.feature.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.dao.WorldBookDAO
import cn.mine.minestars.data.db.entity.WorldBookEntity
import cn.mine.minestars.core.tavern.worldbook.WorldBookEntry
import cn.mine.minestars.core.tavern.worldbook.WorldBookParser
import org.json.JSONArray
import org.json.JSONObject

sealed interface WorldBookDetailEvent {
    data class Success(val message: String) : WorldBookDetailEvent
    data class Error(val message: String) : WorldBookDetailEvent
}

class WorldBookDetailVM(
    private val worldBookDao: WorldBookDAO,
    private val assistantDao: AssistantDAO,
) : ViewModel() {
    private val _bookName = MutableStateFlow("")
    val bookName = _bookName.asStateFlow()

    private val _enabled = MutableStateFlow(true)
    val enabled = _enabled.asStateFlow()

    private val _entries = MutableStateFlow<List<WorldBookEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges = _hasUnsavedChanges.asStateFlow()

    private val _events = MutableSharedFlow<WorldBookDetailEvent>()
    val events = _events.asSharedFlow()

    private var bookId: String = ""
    private var rawJsonObj: JSONObject? = null
    private var isAssistantBook = false
    private var assistantId: String = ""

    fun loadBook(id: String) {
        if (id == bookId && _entries.value.isNotEmpty()) return // already loaded
        isAssistantBook = false
        assistantId = ""
        viewModelScope.launch {
            val entity = worldBookDao.getById(id) ?: return@launch
            bookId = entity.id
            val json = runCatching { JSONObject(entity.rawJson) }.getOrNull() ?: return@launch
            rawJsonObj = json
            _bookName.value = json.optString("name", "")
            _enabled.value = entity.enabled
            _entries.value = WorldBookParser.parse(entity.rawJson)
            _hasUnsavedChanges.value = false
        }
    }

    fun loadAssistantBook(assistantId: String) {
        if (assistantId == this.assistantId && _entries.value.isNotEmpty()) return
        isAssistantBook = true
        this.assistantId = assistantId
        viewModelScope.launch {
            val entity = assistantDao.getById(assistantId) ?: return@launch
            val jsonStr = entity.worldBookJson
            if (jsonStr.isNullOrBlank()) return@launch
            val json = runCatching { JSONObject(jsonStr) }.getOrNull() ?: return@launch
            bookId = assistantId
            rawJsonObj = json
            _bookName.value = json.optString("name", "")
            _enabled.value = json.optBoolean("enabled", true)
            _entries.value = WorldBookParser.parse(jsonStr)
            _hasUnsavedChanges.value = false
        }
    }

    fun updateBookName(v: String) {
        _bookName.value = v
        _hasUnsavedChanges.value = true
    }

    fun toggleEnabled() {
        _enabled.value = !_enabled.value
        _hasUnsavedChanges.value = true
    }

    fun setEntryEnabled(index: Int, enabled: Boolean) {
        val list = _entries.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(enabled = enabled)
            _entries.value = list
        }
        _hasUnsavedChanges.value = true
    }

    fun reorderEntries(from: Int, to: Int) {
        // LazyColumn has "tabs" item at index 0, so the reorderable library
        // reports global indices offset by 1. Convert to entry-list indices.
        val adjustedFrom = from - 1
        val adjustedTo = to - 1
        val list = _entries.value.toMutableList()
        if (adjustedFrom in list.indices && adjustedTo in list.indices) {
            val item = list.removeAt(adjustedFrom)
            list.add(adjustedTo, item)
            _entries.value = list
        }
        _hasUnsavedChanges.value = true
    }

    fun deleteEntry(index: Int) {
        val list = _entries.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _entries.value = list
        }
        _hasUnsavedChanges.value = true
    }

    fun addEntry(): Int {
        val list = _entries.value.toMutableList()
        val maxUid = list.maxOfOrNull { it.uid ?: 0 } ?: 0
        val entry = WorldBookEntry(
            uid = maxUid + 1,
            title = "新条目",
            content = "",
            decorators = emptyList(),
            forceActivate = false,
            forceDisable = false,
            keys = emptyList(),
            secondaryKeys = emptyList(),
            enabled = true,
            constant = false,
            vectorized = false,
            position = 0,
            depth = null,
            order = 100,
            selective = true,
            selectiveLogic = 0,
            useRegex = false,
            scanDepth = null,
            probability = 100,
            useProbability = true,
            group = "",
            groupOverride = false,
            groupWeight = 100,
            sticky = null,
            cooldown = null,
            delay = null,
            caseSensitive = null,
            matchWholeWords = null,
            matchPersonaDescription = false,
            matchCharacterDescription = false,
            matchCharacterPersonality = false,
            matchCharacterDepthPrompt = false,
            matchScenario = false,
            matchCreatorNotes = false,
            delayUntilRecursion = false,
            preventRecursion = false,
            excludeRecursion = false,
            useGroupScoring = null,
            automationId = null,
            role = "system",
            outletName = null,
            triggers = emptyList(),
            ignoreBudget = false,
            characterFilterNames = emptyList(),
            characterFilterTags = emptyList(),
            characterFilterExclude = false,
        )
        list.add(entry)
        _entries.value = list
        _hasUnsavedChanges.value = true
        return list.lastIndex
    }

    /** Load book from DB, add an entry, persist immediately, callback with index. */
    fun addEntryAndPersist(bookId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val entity = worldBookDao.getById(bookId) ?: return@launch
            val json = runCatching { JSONObject(entity.rawJson) }.getOrNull() ?: return@launch
            rawJsonObj = json
            this@WorldBookDetailVM.bookId = bookId
            _bookName.value = json.optString("name", "")
            _enabled.value = entity.enabled
            _entries.value = WorldBookParser.parse(entity.rawJson)

            val index = addEntry()

            // Serialize and persist to DB immediately
            val entriesArray = JSONArray()
            for (e in _entries.value) {
                entriesArray.put(serializeEntry(e))
            }
            if (json.has("character_book")) {
                json.getJSONObject("character_book").put("entries", entriesArray)
            } else if (json.has("data") && json.optJSONObject("data") != null) {
                json.getJSONObject("data").put("entries", entriesArray)
            } else {
                json.put("entries", entriesArray)
            }
            worldBookDao.insert(WorldBookEntity(
                id = bookId,
                name = _bookName.value.ifBlank { "未命名" },
                rawJson = json.toString(),
                enabled = _enabled.value,
            ))
            _hasUnsavedChanges.value = false

            onResult(index)
        }
    }

    fun updateEntry(index: Int, entry: WorldBookEntry) {
        val list = _entries.value.toMutableList()
        if (index in list.indices) {
            list[index] = entry
            _entries.value = list
        }
        _hasUnsavedChanges.value = true
    }

    suspend fun save() {
        try {
            if (isAssistantBook) {
                saveToAssistant()
            } else {
                saveToGlobal()
            }
        } catch (e: Exception) {
            _events.emit(WorldBookDetailEvent.Error("保存失败: ${e.message}"))
        }
    }

    private suspend fun saveToGlobal() {
        val json = rawJsonObj ?: JSONObject()
        json.put("name", _bookName.value)

        val entriesArray = buildEntriesArray()
        putEntries(json, entriesArray)

        worldBookDao.insert(WorldBookEntity(
            id = bookId,
            name = _bookName.value.ifBlank { "未命名" },
            rawJson = json.toString(),
            enabled = _enabled.value,
        ))
        _hasUnsavedChanges.value = false
        _events.emit(WorldBookDetailEvent.Success("已保存"))
    }

    private suspend fun saveToAssistant() {
        val json = JSONObject()
        json.put("name", _bookName.value)
        json.put("enabled", _enabled.value)
        json.put("entries", buildEntriesArray())

        val entity = assistantDao.getById(assistantId) ?: return
        assistantDao.update(entity.copy(worldBookJson = json.toString()))
        _hasUnsavedChanges.value = false
        _events.emit(WorldBookDetailEvent.Success("已保存"))
    }

    private fun buildEntriesArray(): JSONArray {
        val entriesArray = JSONArray()
        for (entry in _entries.value) {
            entriesArray.put(serializeEntry(entry))
        }
        return entriesArray
    }

    private fun putEntries(json: JSONObject, entriesArray: JSONArray) {
        if (json.has("character_book")) {
            val cb = json.getJSONObject("character_book")
            cb.put("entries", entriesArray)
            cb.put("name", _bookName.value)
        } else if (json.has("data") && json.optJSONObject("data") != null) {
            json.getJSONObject("data").put("entries", entriesArray)
        } else {
            json.put("entries", entriesArray)
        }
    }

    private fun serializeEntry(entry: WorldBookEntry): JSONObject {
        val obj = JSONObject()
        obj.put("uid", entry.uid ?: JSONObject.NULL)
        if (entry.keys.isNotEmpty()) obj.put("keys", JSONArray(entry.keys))
        if (entry.secondaryKeys.isNotEmpty()) obj.put("secondary_keys", JSONArray(entry.secondaryKeys))
        if (!entry.title.isNullOrBlank()) obj.put("comment", entry.title)

        val content = buildString {
            if (entry.forceActivate) append("@@activate\n")
            if (entry.forceDisable) append("@@dont_activate\n")
            append(entry.content)
        }
        obj.put("content", content)
        obj.put("constant", entry.constant)
        obj.put("enabled", entry.enabled)
        obj.put("selective", entry.selective)
        if (!entry.useRegex) obj.put("use_regex", false)
        obj.put("selectiveLogic", entry.selectiveLogic ?: 0)

        val ext = JSONObject()
        ext.put("position", entry.position)
        if (entry.depth != null) ext.put("depth", entry.depth)
        ext.put("order", entry.order)
        ext.put("role", when (entry.role) {
            "user" -> 1
            "assistant" -> 2
            else -> 0
        })
        ext.put("vectorized", entry.vectorized)
        if (entry.group.isNotBlank()) ext.put("group", entry.group)
        ext.put("group_override", entry.groupOverride)
        ext.put("group_weight", entry.groupWeight ?: 100)
        if (entry.sticky != null) ext.put("sticky", entry.sticky)
        if (entry.cooldown != null) ext.put("cooldown", entry.cooldown)
        if (entry.delay != null) ext.put("delay", entry.delay)
        if (entry.scanDepth != null) ext.put("scan_depth", entry.scanDepth)
        if (entry.probability != null) ext.put("probability", entry.probability)
        ext.put("use_probability", entry.useProbability)
        if (entry.caseSensitive != null) ext.put("case_sensitive", entry.caseSensitive)
        if (entry.matchWholeWords != null) ext.put("match_whole_words", entry.matchWholeWords)
        if (entry.useGroupScoring != null) ext.put("use_group_scoring", entry.useGroupScoring)
        if (!entry.automationId.isNullOrBlank()) ext.put("automation_id", entry.automationId)
        if (!entry.outletName.isNullOrBlank()) ext.put("outlet_name", entry.outletName)
        if (entry.triggers.isNotEmpty()) ext.put("triggers", JSONArray(entry.triggers))
        ext.put("ignore_budget", entry.ignoreBudget)
        ext.put("exclude_recursion", entry.excludeRecursion)
        ext.put("prevent_recursion", entry.preventRecursion)
        if (entry.delayUntilRecursion) ext.put("delay_until_recursion", true)
        ext.put("match_persona_description", entry.matchPersonaDescription)
        ext.put("match_character_description", entry.matchCharacterDescription)
        ext.put("match_character_personality", entry.matchCharacterPersonality)
        ext.put("match_character_depth_prompt", entry.matchCharacterDepthPrompt)
        ext.put("match_scenario", entry.matchScenario)
        ext.put("match_creator_notes", entry.matchCreatorNotes)
        if (entry.characterFilterNames.isNotEmpty() || entry.characterFilterTags.isNotEmpty()) {
            val filter = JSONObject().apply {
                if (entry.characterFilterNames.isNotEmpty()) put("names", JSONArray(entry.characterFilterNames))
                if (entry.characterFilterTags.isNotEmpty()) put("tags", JSONArray(entry.characterFilterTags))
                put("isExclude", entry.characterFilterExclude)
            }
            ext.put("character_filter", filter)
        }
        obj.put("extensions", ext)
        return obj
    }
}
