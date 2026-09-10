package cn.mine.minestars.data.tavern.pipeline

import cn.mine.ai.core.MessageRole
import cn.mine.ai.ui.UIMessage
import cn.mine.ai.ui.UIMessagePart
import cn.mine.minestars.data.db.dao.PresetDAO as PresetDao
import cn.mine.minestars.data.db.dao.PresetEntryDAO as PresetEntryDao
import cn.mine.minestars.data.db.dao.RegexGroupDAO
import cn.mine.minestars.data.db.dao.RegexScriptDAO as RegexScriptDao
import cn.mine.minestars.data.db.dao.WorldBookDAO as WorldBookDao
import cn.mine.minestars.data.db.entity.PresetEntity
import cn.mine.minestars.data.db.entity.PresetEntryEntity
import cn.mine.minestars.core.tavern.preset.PresetEntryData
import cn.mine.minestars.core.tavern.preset.PresetModelParams
import cn.mine.minestars.core.tavern.worldbook.ChatRequestWorldBookSupport
import cn.mine.minestars.core.tavern.worldbook.WorldBookActivator
import cn.mine.minestars.core.tavern.worldbook.WorldBookParser
import cn.mine.minestars.core.tavern.worldbook.WorldBookScanContext
import cn.mine.minestars.core.tavern.macro.TavernMacroContext
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.utils.JsonInstant
import android.util.Log
import kotlin.uuid.Uuid
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads, parses, and caches Tavern data from the TavernDatabase for use
 * in the chat generation pipeline.
 */
class TavernDataLoader(
    private val presetDao: PresetDao,
    private val presetEntryDao: PresetEntryDao,
    private val worldBookDao: WorldBookDao,
    private val regexScriptDao: RegexScriptDao,
    private val regexGroupDao: RegexGroupDAO,
) {
    suspend fun loadContext(
        assistant: Assistant,
        messages: List<UIMessage>,
        userName: String = "user",
        tavernMacroContext: TavernMacroContext? = null,
    ): Pair<TavernContext, List<UIMessage>> {
        android.util.Log.d("TavernDataLoader", "loadContext: presetId=${assistant.presetId}, worldBookIds=${assistant.worldBookIds}")

        // Phase 1: Load preset data (no dependency on messages)
        val presetEntity = assistant.presetId?.let { runCatching { presetDao.getById(it.toString()) }.getOrNull() }
        val presetModelParams = presetEntity?.let { loadPresetModelParams(it) }
        val promptOrder = parsePromptOrder(presetEntity?.rawJson)

        // Phase 2: Load regex scripts EARLY — needed for the regex pass before WI scan
        val regexScripts = loadRegexScripts(
            assistant = assistant,
            scopedGroupIds = assistant.regexGroupIds,
            presetId = assistant.presetId,
        )

        // Phase 3: Apply regex to ALL chat messages (USER_INPUT + AI_OUTPUT)
        // This MUST happen before WI scan, matching SillyTavern's order:
        //   "Regex scripts applied to EVERY chat message" → "World info scanning"
        val regexdMessages = applyRegexToMessages(messages, regexScripts)

        // Phase 4: Build character card prompt
        // (Macro resolution happens in the assemble phase via PromptOrderAssembler's
        // resolveWithTemplates(), matching ST's substituteParams behavior in prompt assembly.)
        val characterCardPrompt = buildCharacterCardPrompt(
            assistant = assistant,
            descriptionFormat = presetModelParams?.descriptionFormat,
            personaFormat = presetModelParams?.personaFormat,
            personalityFormat = presetModelParams?.personalityFormat,
            scenarioFormat = presetModelParams?.scenarioFormat,
        )

        // Phase 5: World book activation using REGEX'D messages
        // WI keywords now match against post-regex message content (ST behavior).
        val worldBookInjections = loadWorldBookInjections(
            assistant = assistant,
            messages = regexdMessages,
            wiFormat = presetModelParams?.wiFormat ?: "{0}",
        )

        // Phase 6: Load preset entries (no dependency on regex)
        val presetEntries = loadPresetEntries(assistant.presetId)

        val tavernCtx = TavernContext(
            characterCardPrompt = characterCardPrompt,
            presetEntries = presetEntries,
            presetModelParams = presetModelParams,
            worldBookInjections = worldBookInjections,
            regexScripts = regexScripts,
            promptOrder = promptOrder,
            squashSystemMessages = presetModelParams?.squashSystemMessages ?: false,
            wiFormat = presetModelParams?.wiFormat ?: "{0}",
            impersonationPrompt = presetModelParams?.impersonationPrompt ?: "",
            groupNudgePrompt = presetModelParams?.groupNudgePrompt ?: "",
            newChatPrompt = presetModelParams?.newChatPrompt ?: "",
            newGroupChatPrompt = presetModelParams?.newGroupChatPrompt ?: "",
            newExampleChatPrompt = presetModelParams?.newExampleChatPrompt ?: "",
            continueNudgePrompt = presetModelParams?.continueNudgePrompt ?: "",
            sendIfEmptyPrompt = presetModelParams?.sendIfEmptyPrompt ?: "",
        )

        return Pair(tavernCtx, regexdMessages)
    }

    // ---- Character Card ----

    // ---- Preset ----

    private suspend fun loadPresetEntries(presetId: Uuid?): List<PresetEntryData> {
        if (presetId == null) return emptyList()
        val entities = presetEntryDao.getByPresetId(presetId.toString())
        return entities.filter { it.mounted }.map { it.toPresetEntryData() }
    }

    private suspend fun loadPresetModelParams(preset: PresetEntity): PresetModelParams? {
        return try {
            PresetModelParams.fromJson(JSONObject(preset.rawJson))
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        /**
         * Parses the prompt_order JSON array from a preset's raw JSON.
         * Mirrors the group-selection logic in PresetParser.extractEntries().
         * Returns null if no valid prompt_order is found.
         */
        internal fun parsePromptOrder(rawJson: String?): List<PromptOrderItem>? {
            if (rawJson.isNullOrBlank()) return null
            val preset = try { JSONObject(rawJson) } catch (_: Exception) { return null }
            val promptOrder = preset.optJSONArray("prompt_order") ?: return null
            if (promptOrder.length() == 0) return null

            val selectedOrderArray = run {
                val firstItem = promptOrder.optJSONObject(0)
                if (firstItem != null && firstItem.has("identifier")) {
                    return@run promptOrder
                }
                var preferred100001: JSONObject? = null
                var fallback100000: JSONObject? = null
                var first: JSONObject? = null
                for (i in 0 until promptOrder.length()) {
                    val group = promptOrder.optJSONObject(i) ?: continue
                    if (first == null) first = group
                    when (group.optInt("character_id", Int.MIN_VALUE)) {
                        100001 -> preferred100001 = group
                        100000 -> if (fallback100000 == null) fallback100000 = group
                    }
                }
                (preferred100001 ?: fallback100000 ?: first)?.optJSONArray("order")
            } ?: return null

            val items = mutableListOf<PromptOrderItem>()
            for (i in 0 until selectedOrderArray.length()) {
                val orderItem = selectedOrderArray.opt(i)
                val identifier = when (orderItem) {
                    is JSONObject -> orderItem.optString("identifier")
                    is String -> orderItem
                    else -> null
                }?.takeIf { it.isNotBlank() } ?: continue
                val enabled = if (orderItem is JSONObject && orderItem.has("enabled")) {
                    orderItem.optBoolean("enabled", true)
                } else {
                    true
                }
                items.add(PromptOrderItem(identifier = identifier, enabled = enabled))
            }
            return items.takeIf { it.isNotEmpty() }
        }

        fun buildCharacterCardPrompt(
            assistant: Assistant,
            descriptionFormat: String? = "{{description}}",
            personaFormat: String? = "{{persona}}",
            personalityFormat: String? = "{{personality}}",
            scenarioFormat: String? = "{{scenario}}",
        ): String? {
            val hasCard = assistant.charName.isNotBlank()
                || assistant.description.isNotBlank()
                || assistant.personality.isNotBlank()
                || assistant.scenario.isNotBlank()
                || assistant.mesExamples.isNotBlank()
                || assistant.postHistoryInstructions.isNotBlank()
            if (!hasCard) return null

            val charName = assistant.charName.ifBlank { assistant.name }
            val persona = assistant.description  // persona = description in our model

            return buildString {
                // Description / Persona block
                if (descriptionFormat != null && assistant.description.isNotBlank()) {
                    val line = descriptionFormat
                        .replace("{{description}}", assistant.description)
                        .replace("{{persona}}", persona)
                        .replace("{{char}}", charName)
                    if (line.isNotBlank()) {
                        appendLine(line)
                        appendLine()
                    }
                }
                // Personality block
                if (personalityFormat != null && assistant.personality.isNotBlank()) {
                    val line = personalityFormat
                        .replace("{{personality}}", assistant.personality)
                        .replace("{{char}}", charName)
                    if (line.isNotBlank()) {
                        appendLine(line)
                        appendLine()
                    }
                }
                // Scenario block
                if (scenarioFormat != null && assistant.scenario.isNotBlank()) {
                    val line = scenarioFormat
                        .replace("{{scenario}}", assistant.scenario)
                        .replace("{{char}}", charName)
                    if (line.isNotBlank()) {
                        appendLine(line)
                        appendLine()
                    }
                }

                // Example messages
                if (assistant.mesExamples.isNotBlank()) {
                    appendLine("[Example messages:]")
                    appendLine(assistant.mesExamples)
                    appendLine()
                }
                // Post-history instructions
                if (assistant.postHistoryInstructions.isNotBlank()) {
                    appendLine("[Post-History Instructions:]")
                    appendLine(assistant.postHistoryInstructions)
                }
            }.trimEnd().takeIf { it.isNotBlank() }
        }

        // PresetEntryEntity → PresetEntryData mapper
        private fun PresetEntryEntity.toPresetEntryData(): PresetEntryData = PresetEntryData(
            id = id,
            identifier = identifier,
            name = name,
            enabled = enabled,
            role = role,
            content = content,
            injectionPosition = injectionPosition,
            injectionDepth = injectionDepth,
            injectionOrder = injectionOrder,
            systemPrompt = systemPrompt,
            marker = marker,
            forbidOverrides = forbidOverrides,
            injectionTrigger = parseInjectionTrigger(injectionTriggerJson),
            mounted = mounted,
        )

        private fun parseInjectionTrigger(json: String): List<String> {
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { arr.optString(it) }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    // ---- World Book ----

    private suspend fun loadWorldBookInjections(
        assistant: Assistant,
        messages: List<UIMessage>,
        wiFormat: String = "{0}",
    ): List<WorldBookInjection> {
        val hasGlobalBooks = assistant.worldBookIds.isNotEmpty()
        val hasAssistantBook = !assistant.worldBookJson.isNullOrBlank()

        if (!hasGlobalBooks && !hasAssistantBook) return emptyList()

        // Parse entries from global world books (referenced by assistant.worldBookIds)
        val globalEntries = assistant.worldBookIds.mapNotNull { id ->
            runCatching { worldBookDao.getById(id.toString()) }.getOrNull()
        }.filter { it.enabled }.flatMap { entity ->
            WorldBookParser.parse(entity.rawJson)
        }

        // Parse entries from assistant-level world book (Layer 2, embedded JSON)
        val assistantEntries = if (hasAssistantBook) {
            val obj = runCatching { JSONObject(assistant.worldBookJson) }.getOrNull()
            if (obj != null && obj.optBoolean("enabled", true)) {
                WorldBookParser.parse(assistant.worldBookJson)
            } else emptyList()
        } else emptyList()

        val allEntries = globalEntries + assistantEntries
        if (allEntries.isEmpty()) return emptyList()

        // Build scan context for the activator with available character data
        val nonSystemMessages = messages.filter { it.role != cn.mine.ai.core.MessageRole.SYSTEM }
        val scanContext = WorldBookScanContext(
            recentMessages = nonSystemMessages.map { it.toText() },
            currentMessage = "",
            personaDescription = "",
            characterDescription = assistant.description,
            characterPersonality = assistant.personality,
            characterDepthPrompt = "",
            scenario = assistant.scenario,
            creatorNotes = assistant.creatorNotes,
            characterName = assistant.charName,
            characterTags = assistant.cardTags,
        )

        // Use the core module's full SillyTavern-compatible activation pipeline:
        // keyword matching (regex/whole-word/case-sensitive), primary+secondary key logic,
        // group scoring, sticky/cooldown/delay, character filtering, recursion, probability, etc.
        val activated = WorldBookActivator.activate(
            entries = allEntries,
            context = scanContext,
            settings = ChatRequestWorldBookSupport.WorldBookScanSettings(),
            trigger = "normal",
        )

        return activated.map { entry ->
            val injectPosition = when (entry.position ?: 4) {
                0 -> InjectionPosition.BEFORE_PROMPT    // standard before-prompt
                1, 3, 6 -> InjectionPosition.AFTER_PROMPT // after, AN_bottom, EM_bottom → after
                2, 5 -> InjectionPosition.BEFORE_PROMPT   // AN_top, EM_top → before
                4 -> InjectionPosition.AT_DEPTH           // standard in-chat depth
                7 -> InjectionPosition.AT_DEPTH           // outlet → depth (no outlet support yet)
                else -> InjectionPosition.AT_DEPTH
            }

            val formattedContent = if (wiFormat.isNotBlank() && wiFormat != "{0}") {
                wiFormat.replace("{0}", entry.content)
            } else {
                entry.content
            }

            WorldBookInjection(
                content = formattedContent,
                role = entry.role,
                depth = entry.depth ?: 4,
                order = entry.order,
                position = injectPosition,
            )
        }
    }

    // ---- Regex Script ----

    private suspend fun loadRegexScripts(
        assistant: Assistant,
        scopedGroupIds: Set<Uuid>,
        presetId: Uuid?,
    ): List<TavernRegexScript> {
        // Layer 1: Preset regex scripts
        val presetScripts = loadPresetRegexScripts(presetId)

        // Layer 2: Assistant-level regex scripts (embedded JSON)
        val assistantScripts = parseAssistantRegexScripts(assistant.regexScriptsJson)
            .filter { it.enabled }
            .mapNotNull { parseRegexScript(it.rawJson) }

        // Layer 3: Global custom regex scripts from bound groups
        val scriptIdsFromGroups = scopedGroupIds.mapNotNull { groupId ->
            regexGroupDao.getById(groupId.toString())
        }.flatMap { group ->
            runCatching {
                JsonInstant.decodeFromString<List<String>>(group.regexIdsJson)
            }.getOrDefault(emptyList())
        }.toSet()

        val allStoredScripts = regexScriptDao.getAll()
        val globalScripts = allStoredScripts
            .filter { it.id in scriptIdsFromGroups }
            .mapNotNull { parseRegexScript(it.rawJson) }

        return (presetScripts + assistantScripts + globalScripts)
            .distinctBy { listOf(it.scriptName, it.findRegex, it.replaceString, it.placement.joinToString(",")).joinToString("|") }
            .filter { !it.disabled }
    }

    private fun parseAssistantRegexScripts(json: String?): List<ScriptRef> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val trimmed = json.trim()
            val arr = if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                if (!root.optBoolean("enabled", true)) return@runCatching emptyList<ScriptRef>()
                root.optJSONArray("scripts")
            } else {
                JSONArray(trimmed)
            }
            if (arr == null) return@runCatching emptyList<ScriptRef>()
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ScriptRef(
                    id = obj.optString("id", ""),
                    rawJson = obj.optString("rawJson", "{}"),
                    enabled = obj.optBoolean("enabled", true),
                )
            }
        }.getOrDefault(emptyList())
    }

    private data class ScriptRef(
        val id: String,
        val rawJson: String,
        val enabled: Boolean,
    )

    internal fun parseRegexScript(rawJson: String): TavernRegexScript? {
        return try {
            val obj = JSONObject(rawJson)
            TavernRegexScript(
                scriptName = obj.optString("scriptName", ""),
                findRegex = obj.optString("findRegex", ""),
                replaceString = obj.optString("replaceString", ""),
                trimStrings = obj.optJSONArray("trimStrings")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
                } ?: emptyList(),
                disabled = obj.optBoolean("disabled", false),
                promptOnly = obj.optBoolean("promptOnly", false),
                markdownOnly = obj.optBoolean("markdownOnly", false),
                substituteRegex = obj.optInt("substituteRegex", 0),
                placement = obj.optJSONArray("placement")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optInt(it).takeIf { value -> value != 0 || !arr.isNull(it) } }
                } ?: emptyList(),
                runOnEdit = obj.optBoolean("runOnEdit", true),
                minDepth = if (obj.has("minDepth")) obj.optInt("minDepth") else null,
                maxDepth = if (obj.has("maxDepth")) obj.optInt("maxDepth") else null,
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun loadPresetRegexScripts(presetId: Uuid?): List<TavernRegexScript> {
        if (presetId == null) return emptyList()
        val preset = presetDao.getById(presetId.toString()) ?: return emptyList()
        val root = runCatching { JSONObject(preset.rawJson) }.getOrNull() ?: return emptyList()
        val extensions = root.optJSONObject("extensions") ?: return emptyList()
        return buildList {
            extensions.optJSONArray("regex_scripts")?.let { array ->
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let { node ->
                        parseRegexScript(node.toString())?.let(::add)
                    }
                }
            }
            extensions.optJSONObject("SPreset")
                ?.optJSONObject("RegexBinding")
                ?.optJSONArray("regexes")
                ?.let { array ->
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { node ->
                            parseRegexScript(node.toString())?.let(::add)
                        }
                    }
                }
        }
    }

    /**
     * Applies USER_INPUT and AI_OUTPUT regex scripts to all non-SYSTEM messages.
     * This is the EARLY regex pass that MUST run before world book activation,
     * matching SillyTavern's order of operations.
     *
     * - USER messages get USER_INPUT placement
     * - ASSISTANT messages get AI_OUTPUT placement
     * - SYSTEM messages are skipped (no applicable placement)
     * - markdownOnly scripts are excluded (isMarkdown=false)
     * - promptOnly scripts run (isPrompt=true, matching ST prompt-context behavior)
     * - default scripts are excluded (isPrompt=true, matching ST prompt-context behavior)
     */
    private fun applyRegexToMessages(
        messages: List<UIMessage>,
        scripts: List<TavernRegexScript>,
    ): List<UIMessage> {
        if (scripts.isEmpty()) return messages
        return messages.map { message ->
            val placement = when (message.role) {
                MessageRole.USER -> TavernRegexPlacement.USER_INPUT
                MessageRole.ASSISTANT -> TavernRegexPlacement.AI_OUTPUT
                else -> return@map message  // SYSTEM: skip
            }
            val rawText = message.toText()
            val regexd = TavernRegexRunner.apply(
                text = rawText,
                scripts = scripts,
                placement = placement,
                macroContext = null,  // tavernMacroCtx not useful here; regex runs on raw text
                isPrompt = true,
                depth = null,  // depth not known at this stage
            )
            if (regexd == rawText) message
            else message.copy(parts = listOf(UIMessagePart.Text(regexd)))
        }
    }
}
