package cn.mine.minestars.feature.tavern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import cn.mine.minestars.data.db.dao.PresetDAO
import cn.mine.minestars.data.db.dao.PresetEntryDAO
import cn.mine.minestars.data.db.entity.PresetEntryEntity
import cn.mine.minestars.core.tavern.preset.PresetModelParams
import cn.mine.minestars.core.tavern.regex.RegexScript
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface PresetDetailEvent {
    data class Success(val message: String) : PresetDetailEvent
    data class Error(val message: String) : PresetDetailEvent
}

class PresetDetailVM(
    private val presetDao: PresetDAO,
    private val presetEntryDao: PresetEntryDAO,
) : ViewModel() {
    private var _presetId: String = ""

    private val _presetName = MutableStateFlow("")
    val presetName = _presetName.asStateFlow()

    private val _entries = MutableStateFlow<List<PresetEntryEntity>>(emptyList())
    val entries = _entries.asStateFlow()

    private val _modelParams = MutableStateFlow(PresetModelParams.defaults())
    val modelParams = _modelParams.asStateFlow()

    private val _presetRegexNames = MutableStateFlow<List<String>>(emptyList())
    val presetRegexNames = _presetRegexNames.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges = _hasUnsavedChanges.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<PresetDetailEvent>()
    val events = _events.asSharedFlow()

    fun loadPreset(id: String) {
        if (id == _presetId && _entries.value.isNotEmpty()) return // already loaded
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val preset = presetDao.getById(id) ?: return@launch
                _presetId = id
                _presetName.value = preset.name
                _entries.value = presetEntryDao.getByPresetId(id).sortedBy { it.entryIndex }
                _modelParams.value = try {
                    PresetModelParams.fromJson(JSONObject(preset.rawJson))
                } catch (_: Exception) {
                    PresetModelParams.defaults()
                }
                _presetRegexNames.value = extractPresetRegexNames(preset.rawJson)
                _hasUnsavedChanges.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** All modifying operations below are memory-only. Call [save] to persist. */

    // ── Entry operations ──

    fun setEntryEnabled(entryIndex: Int, enabled: Boolean) {
        _entries.value = _entries.value.map {
            if (it.entryIndex == entryIndex) it.copy(enabled = enabled) else it
        }
        _hasUnsavedChanges.value = true
    }

    fun reorderMounted(fromMountedIndex: Int, toMountedIndex: Int) {
        val current = _entries.value
        val mounted = current.filter { it.mounted }.toMutableList()

        if (fromMountedIndex !in mounted.indices || toMountedIndex !in mounted.indices) return

        val moved = mounted.removeAt(fromMountedIndex)
        mounted.add(toMountedIndex, moved)

        _entries.value = normalizeEntryOrder(mounted + current.filterNot { it.mounted })
        _hasUnsavedChanges.value = true
    }

    fun mountEntry(entryIndex: Int) {
        val entry = _entries.value.find { it.entryIndex == entryIndex } ?: return
        if (entry.mounted) return

        val mounted = listOf(entry.copy(mounted = true, enabled = false)) +
            _entries.value.filter { it.mounted }
        val unmounted = _entries.value.filterNot { it.mounted || it.entryIndex == entryIndex }

        _entries.value = normalizeEntryOrder(mounted + unmounted)
        _hasUnsavedChanges.value = true
    }

    fun unmountEntry(entryIndex: Int) {
        val entry = _entries.value.find { it.entryIndex == entryIndex } ?: return
        if (!entry.mounted || !canUnmountEntry(entry)) return

        val updated = _entries.value.map {
            if (it.entryIndex == entryIndex) {
                it.copy(mounted = false, enabled = false)
            } else {
                it
            }
        }

        _entries.value = normalizeEntryOrder(updated)
        _hasUnsavedChanges.value = true
    }

    fun deleteEntry(entryIndex: Int) {
        val entry = _entries.value.find { it.entryIndex == entryIndex } ?: return
        if (entry.systemPrompt) {
            viewModelScope.launch {
                _events.emit(PresetDetailEvent.Error("系统提示词条目不可删除"))
            }
            return
        }
        _entries.value = _entries.value.filter { it.entryIndex != entryIndex }
        _hasUnsavedChanges.value = true
    }

    fun addEntry() {
        val currentEntries = _entries.value
        if (currentEntries.isEmpty()) return
        val presetId = currentEntries.first().presetId
        val nextIndex = (currentEntries.maxOfOrNull { it.entryIndex } ?: -1) + 1
        val id = UUID.randomUUID().toString()
        val newEntry = PresetEntryEntity(
            presetId = presetId,
            entryIndex = nextIndex,
            id = id,
            identifier = "custom_$id",
            name = "新建条目",
            enabled = false,
            role = "system",
            content = "",
            injectionPosition = null,
            injectionDepth = null,
            injectionOrder = null,
            systemPrompt = false,
            marker = false,
            forbidOverrides = false,
            injectionTriggerJson = "[]",
            mounted = true,
        )
        _entries.value = _entries.value + newEntry
        _hasUnsavedChanges.value = true
    }

    fun renamePreset(newName: String) {
        _presetName.value = newName
        _hasUnsavedChanges.value = true
    }

    fun updateEntry(entryIndex: Int, updated: PresetEntryEntity) {
        _entries.value = _entries.value.map {
            if (it.entryIndex == entryIndex) updated else it
        }
        _hasUnsavedChanges.value = true
    }

    // ── Model parameter operations ──

    fun updateTemperature(value: Float?) {
        _modelParams.value = _modelParams.value.copy(temperature = value)
        _hasUnsavedChanges.value = true
    }

    fun updateTopP(value: Float?) {
        _modelParams.value = _modelParams.value.copy(topP = value)
        _hasUnsavedChanges.value = true
    }

    fun updateMaxTokens(value: Int?) {
        _modelParams.value = _modelParams.value.copy(maxTokens = value)
        _hasUnsavedChanges.value = true
    }

    fun updateMaxContext(value: Int?) {
        _modelParams.value = _modelParams.value.copy(maxContext = value)
        _hasUnsavedChanges.value = true
    }

    fun updateStreamOutput(value: Boolean) {
        _modelParams.value = _modelParams.value.copy(streamOutput = value)
        _hasUnsavedChanges.value = true
    }

    fun updateReasoningLevel(value: cn.mine.ai.core.ReasoningLevel) {
        _modelParams.value = _modelParams.value.copy(reasoningLevel = value.name.lowercase())
        _hasUnsavedChanges.value = true
    }

    fun updateContinuePrefill(value: Boolean) {
        _modelParams.value = _modelParams.value.copy(continuePrefill = value)
        _hasUnsavedChanges.value = true
    }

    fun updateSquashSystemMessages(value: Boolean) {
        _modelParams.value = _modelParams.value.copy(squashSystemMessages = value)
        _hasUnsavedChanges.value = true
    }

    fun updateFunctionCalling(value: Boolean) {
        _modelParams.value = _modelParams.value.copy(functionCalling = value)
        _hasUnsavedChanges.value = true
    }

    fun updateSendInlineMedia(value: Boolean) {
        _modelParams.value = _modelParams.value.copy(sendInlineMedia = value)
        _hasUnsavedChanges.value = true
    }

    fun updateTopK(value: Float?) {
        _modelParams.value = _modelParams.value.copy(topK = value)
        _hasUnsavedChanges.value = true
    }

    fun updateMinP(value: Float?) {
        _modelParams.value = _modelParams.value.copy(minP = value)
        _hasUnsavedChanges.value = true
    }

    fun updateTopA(value: Float?) {
        _modelParams.value = _modelParams.value.copy(topA = value)
        _hasUnsavedChanges.value = true
    }

    fun updateRepetitionPenalty(value: Float?) {
        _modelParams.value = _modelParams.value.copy(repetitionPenalty = value)
        _hasUnsavedChanges.value = true
    }

    fun updateFrequencyPenalty(value: Float?) {
        _modelParams.value = _modelParams.value.copy(frequencyPenalty = value)
        _hasUnsavedChanges.value = true
    }

    fun updatePresencePenalty(value: Float?) {
        _modelParams.value = _modelParams.value.copy(presencePenalty = value)
        _hasUnsavedChanges.value = true
    }

    fun updateSeed(value: Long?) {
        _modelParams.value = _modelParams.value.copy(seed = value)
        _hasUnsavedChanges.value = true
    }

    fun updateN(value: Int?) {
        _modelParams.value = _modelParams.value.copy(n = value)
        _hasUnsavedChanges.value = true
    }

    fun updateStop(value: List<String>) {
        _modelParams.value = _modelParams.value.copy(stop = value)
        _hasUnsavedChanges.value = true
    }

    fun updateCharacterNamesBehavior(value: Int) {
        _modelParams.value = _modelParams.value.copy(characterNamesBehavior = value)
        _hasUnsavedChanges.value = true
    }

    fun updateDescriptionFormat(value: String) {
        _modelParams.value = _modelParams.value.copy(descriptionFormat = value)
        _hasUnsavedChanges.value = true
    }

    fun updatePersonaFormat(value: String) {
        _modelParams.value = _modelParams.value.copy(personaFormat = value)
        _hasUnsavedChanges.value = true
    }

    fun updatePersonalityFormat(value: String) {
        _modelParams.value = _modelParams.value.copy(personalityFormat = value)
        _hasUnsavedChanges.value = true
    }

    fun updateScenarioFormat(value: String) {
        _modelParams.value = _modelParams.value.copy(scenarioFormat = value)
        _hasUnsavedChanges.value = true
    }

    fun updateWiFormat(value: String) {
        _modelParams.value = _modelParams.value.copy(wiFormat = value)
        _hasUnsavedChanges.value = true
    }

    fun updateImpersonationPrompt(value: String) {
        _modelParams.value = _modelParams.value.copy(impersonationPrompt = value)
        _hasUnsavedChanges.value = true
    }

    fun updateGroupNudgePrompt(value: String) {
        _modelParams.value = _modelParams.value.copy(groupNudgePrompt = value)
        _hasUnsavedChanges.value = true
    }

    fun updateNewChatPrompt(value: String) {
        _modelParams.value = _modelParams.value.copy(newChatPrompt = value)
        _hasUnsavedChanges.value = true
    }

    fun updateNewGroupChatPrompt(value: String) {
        _modelParams.value = _modelParams.value.copy(newGroupChatPrompt = value)
        _hasUnsavedChanges.value = true
    }

    fun updateNewExampleChatPrompt(value: String) {
        _modelParams.value = _modelParams.value.copy(newExampleChatPrompt = value)
        _hasUnsavedChanges.value = true
    }

    fun updateContinueNudgePrompt(value: String) {
        _modelParams.value = _modelParams.value.copy(continueNudgePrompt = value)
        _hasUnsavedChanges.value = true
    }

    fun updateSendIfEmptyPrompt(value: String) {
        _modelParams.value = _modelParams.value.copy(sendIfEmptyPrompt = value)
        _hasUnsavedChanges.value = true
    }

    // ── Regex script operations ──

    fun addRegexScript() {
        val scripts = _modelParams.value.regexScripts.toMutableList()
        scripts.add(
            RegexScript(
                id = UUID.randomUUID().toString(),
                scriptName = "新建正则 ${scripts.size + 1}",
                findRegex = "",
                replaceString = "",
                trimStrings = emptyList(),
                disabled = false,
                promptOnly = false,
                substituteRegex = 0,
                minDepth = null,
                maxDepth = null,
                runOnEdit = true,
                sources = setOf(cn.mine.minestars.core.tavern.regex.RegexSource.USER_INPUT, cn.mine.minestars.core.tavern.regex.RegexSource.AI_OUTPUT),
                destinations = setOf(cn.mine.minestars.core.tavern.regex.RegexDestination.DISPLAY, cn.mine.minestars.core.tavern.regex.RegexDestination.PROMPT),
                placement = listOf(1, 2),
                origin = "preset",
            )
        )
        _modelParams.value = _modelParams.value.copy(regexScripts = scripts)
        _hasUnsavedChanges.value = true
    }

    fun updateRegexScript(id: String, script: RegexScript) {
        val scripts = _modelParams.value.regexScripts.toMutableList()
        val index = scripts.indexOfFirst { it.id == id }
        if (index >= 0) {
            scripts[index] = script
            _modelParams.value = _modelParams.value.copy(regexScripts = scripts)
            _hasUnsavedChanges.value = true
        }
    }

    fun removeRegexScript(id: String) {
        val scripts = _modelParams.value.regexScripts.toMutableList()
        if (scripts.removeAll { it.id == id }) {
            _modelParams.value = _modelParams.value.copy(regexScripts = scripts)
            _hasUnsavedChanges.value = true
        }
    }

    fun reorderRegexScript(fromIndex: Int, toIndex: Int) {
        val scripts = _modelParams.value.regexScripts.toMutableList()
        if (fromIndex in scripts.indices && toIndex in scripts.indices) {
            val moved = scripts.removeAt(fromIndex)
            scripts.add(toIndex, moved)
            _modelParams.value = _modelParams.value.copy(regexScripts = scripts)
            _hasUnsavedChanges.value = true
        }
    }

    fun getRegexScriptById(id: String): RegexScript? {
        return _modelParams.value.regexScripts.firstOrNull { it.id == id }
    }

    /** Expose the raw regex scripts list for the list page. */
    val regexScripts: List<RegexScript>
        get() = _modelParams.value.regexScripts

    fun toggleRegexScriptsEnabled() {
        _modelParams.value = _modelParams.value.copy(regexScriptsEnabled = !_modelParams.value.regexScriptsEnabled)
        _hasUnsavedChanges.value = true
    }

    /** Write all in-memory changes to DB at once. */
    suspend fun save() {
        try {
            val current = _entries.value
            if (current.isEmpty()) return
            val pid = _presetId.ifBlank { current.first().presetId }

            val preset = presetDao.getById(pid) ?: return
            val json = try {
                JSONObject(preset.rawJson)
            } catch (_: Exception) {
                JSONObject()
            }

            // Update name
            json.put("name", _presetName.value)

            _modelParams.value.applyToJson(json)

            json.put("prompts", buildPromptsJson(json.optJSONArray("prompts"), current))
            json.put("prompt_order", buildPromptOrderJson(current))

            presetDao.insert(preset.copy(
                name = _presetName.value,
                rawJson = json.toString(),
            ))

            // Replace all entries
            presetEntryDao.deleteByPresetId(pid)
            presetEntryDao.insertAll(current)

            _hasUnsavedChanges.value = false
            _events.emit(PresetDetailEvent.Success("已保存"))
        } catch (e: Exception) {
            _events.emit(PresetDetailEvent.Error("保存失败: ${e.message}"))
        }
    }

    private fun normalizeEntryOrder(entries: List<PresetEntryEntity>): List<PresetEntryEntity> {
        val mounted = entries.filter { it.mounted }
        val unmounted = entries.filterNot { it.mounted }
        return (mounted + unmounted).mapIndexed { index, entry ->
            entry.copy(entryIndex = index)
        }
    }

    private fun canUnmountEntry(entry: PresetEntryEntity): Boolean {
        return !entry.systemPrompt
    }

    private fun buildPromptOrderJson(entries: List<PresetEntryEntity>): JSONArray {
        val order = JSONArray()
        entries
            .filter { it.mounted }
            .sortedBy { it.entryIndex }
            .forEach { entry ->
                order.put(
                    JSONObject().apply {
                        put("identifier", entry.identifier)
                        put("enabled", entry.enabled)
                    }
                )
            }

        return JSONArray().put(
            JSONObject().apply {
                put("character_id", 100000)
                put("order", order)
            }
        )
    }

    private fun buildPromptsJson(originalPrompts: JSONArray?, entries: List<PresetEntryEntity>): JSONArray {
        val originalByIdentifier = linkedMapOf<String, JSONObject>()
        if (originalPrompts != null) {
            for (index in 0 until originalPrompts.length()) {
                val prompt = originalPrompts.optJSONObject(index) ?: continue
                val identifier = prompt.optString("identifier")
                if (identifier.isNotBlank() && !originalByIdentifier.containsKey(identifier)) {
                    originalByIdentifier[identifier] = prompt
                }
            }
        }

        val prompts = JSONArray()
        entries
            .sortedBy { it.entryIndex }
            .forEach { entry ->
                prompts.put(
                    (originalByIdentifier[entry.identifier] ?: JSONObject()).apply {
                        put("identifier", entry.identifier)
                        put("name", entry.name)
                        put("system_prompt", entry.systemPrompt)
                        put("marker", entry.marker)
                        if (entry.role.isNotBlank()) {
                            put("role", entry.role)
                        } else {
                            remove("role")
                        }
                        put("content", entry.content)
                        putNullableInt("injection_position", entry.injectionPosition)
                        putNullableInt("injection_depth", entry.injectionDepth)
                        putNullableInt("injection_order", entry.injectionOrder)
                        if (entry.forbidOverrides) {
                            put("forbid_overrides", true)
                        } else {
                            remove("forbid_overrides")
                        }
                        put("injection_trigger", parseTriggerJson(entry.injectionTriggerJson))
                    }
                )
            }
        return prompts
    }

    private fun parseTriggerJson(value: String): JSONArray {
        if (value.isBlank()) return JSONArray()
        return try {
            JSONArray(value)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun JSONObject.putNullableInt(key: String, value: Int?) {
        if (value == null) {
            remove(key)
        } else {
            put(key, value)
        }
    }

    private fun extractPresetRegexNames(rawJson: String): List<String> {
        val root = runCatching { JSONObject(rawJson) }.getOrNull() ?: return emptyList()
        val extensions = root.optJSONObject("extensions") ?: return emptyList()
        return buildList {
            extensions.optJSONArray("regex_scripts")?.let { array ->
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.regexName(index)?.let(::add)
                }
            }
            extensions.optJSONObject("SPreset")
                ?.optJSONObject("RegexBinding")
                ?.optJSONArray("regexes")
                ?.let { array ->
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.regexName(index)?.let(::add)
                    }
                }
        }.distinct()
    }

    private fun JSONObject.regexName(index: Int): String {
        return optString("scriptName")
            .ifBlank { optString("name") }
            .ifBlank { "预设正则 ${index + 1}" }
    }
}
