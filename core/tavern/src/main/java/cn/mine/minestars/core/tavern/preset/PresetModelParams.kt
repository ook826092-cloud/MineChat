package cn.mine.minestars.core.tavern.preset

import cn.mine.minestars.core.tavern.regex.RegexScript
import org.json.JSONArray
import org.json.JSONObject

/**
 * Model parameters stored inside the preset's rawJson.
 * JSON keys are 100% aligned with SillyTavern's OpenAI preset format.
 */
data class PresetModelParams(
    // ── Basic sampling parameters ──
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Float? = null,
    val minP: Float? = null,
    val topA: Float? = null,
    val repetitionPenalty: Float? = null,
    val frequencyPenalty: Float? = null,
    val presencePenalty: Float? = null,
    val seed: Long? = null,
    val n: Int? = null,
    val maxTokens: Int? = null,
    val stop: List<String> = emptyList(),

    // ── Context & response ──
    val streamOutput: Boolean = true,
    val reasoningLevel: String = "auto",

    // ── Naming behavior (ST: names_behavior, 0=default, 1=completion, 2=content, -1=none) ──
    val characterNamesBehavior: Int = 0,

    // ── Context budget ──
    val maxContext: Int? = null,            // openai_max_context (ST key), max context token budget

    // ── Config switches ──
    val continuePrefill: Boolean = false,
    val squashSystemMessages: Boolean = false,
    val functionCalling: Boolean = false,
    val sendInlineMedia: Boolean = false,

    // ── Character format templates (root level, ST convention) ──
    val descriptionFormat: String = DEFAULT_DESCRIPTION_FORMAT,
    val personaFormat: String = DEFAULT_PERSONA_FORMAT,
    val personalityFormat: String = DEFAULT_PERSONALITY_FORMAT,
    val scenarioFormat: String = DEFAULT_SCENARIO_FORMAT,

    // ── Regex scripts (stored in extensions.regex_scripts) ──
    val regexScriptsEnabled: Boolean = true,
    val regexScripts: List<RegexScript> = emptyList(),

    // ── Utility Prompts (root level, matching ST) ──
    val wiFormat: String = DEFAULT_WI_FORMAT,
    val impersonationPrompt: String = DEFAULT_IMPERSONATION_PROMPT,
    val groupNudgePrompt: String = DEFAULT_GROUP_NUDGE_PROMPT,
    val newChatPrompt: String = DEFAULT_NEW_CHAT_PROMPT,
    val newGroupChatPrompt: String = DEFAULT_NEW_GROUP_CHAT_PROMPT,
    val newExampleChatPrompt: String = DEFAULT_NEW_EXAMPLE_CHAT_PROMPT,
    val continueNudgePrompt: String = DEFAULT_CONTINUE_NUDGE_PROMPT,
    val sendIfEmptyPrompt: String = DEFAULT_SEND_IF_EMPTY,
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        // ── Basic sampling (ST keys) ──
        temperature?.let { obj.put("temperature", it) }
        topP?.let { obj.put("top_p", it) }
        topK?.let { obj.put("top_k", it) }
        minP?.let { obj.put("min_p", it) }
        topA?.let { obj.put("top_a", it) }
        repetitionPenalty?.let { obj.put("repetition_penalty", it) }
        frequencyPenalty?.let { obj.put("frequency_penalty", it) }
        presencePenalty?.let { obj.put("presence_penalty", it) }
        seed?.let { obj.put("seed", it) }
        n?.let { obj.put("n", it) }
        maxTokens?.let { obj.put("openai_max_tokens", it) }
        maxContext?.let { obj.put("openai_max_context", it) }
        if (stop.isNotEmpty()) obj.put("stop", JSONArray(stop))

        obj.put("stream_openai", streamOutput)
        obj.put("reasoning_effort", reasoningLevel)
        obj.put("names_behavior", characterNamesBehavior)

        // Config switches
        obj.put("continue_prefill", continuePrefill)
        obj.put("squash_system_messages", squashSystemMessages)
        obj.put("function_calling", functionCalling)
        obj.put("media_inlining", sendInlineMedia)

        // ── Format fields and utility prompts at root level ──
        obj.put("description_format", descriptionFormat)
        obj.put("persona_format", personaFormat)
        obj.put("personality_format", personalityFormat)
        obj.put("scenario_format", scenarioFormat)
        obj.put("wi_format", wiFormat)
        obj.put("impersonation_prompt", impersonationPrompt)
        obj.put("group_nudge_prompt", groupNudgePrompt)
        obj.put("new_chat_prompt", newChatPrompt)
        obj.put("new_group_chat_prompt", newGroupChatPrompt)
        obj.put("new_example_chat_prompt", newExampleChatPrompt)
        obj.put("continue_nudge_prompt", continueNudgePrompt)
        obj.put("send_if_empty", sendIfEmptyPrompt)

        // ── Extensions: regex scripts ──
        if (regexScripts.isNotEmpty()) {
            val extensions = obj.optJSONObject("extensions") ?: JSONObject()
            extensions.put("regex_scripts", JSONArray(regexScripts.map { it.toJson() }))
            extensions.put("regex_scripts_enabled", regexScriptsEnabled)
            obj.put("extensions", extensions)
        }

        return obj
    }

    fun applyToJson(obj: JSONObject) {
        // Remove old MR-exclusive keys (pre-ST-alignment format)
        removeOldKeys(obj)

        // ── Basic sampling (ST keys) ──
        putNumberOrRemove(obj, temperature, "temperature")
        putNumberOrRemove(obj, topP, "top_p")
        putNumberOrRemove(obj, topK, "top_k")
        putNumberOrRemove(obj, minP, "min_p")
        putNumberOrRemove(obj, topA, "top_a")
        putNumberOrRemove(obj, repetitionPenalty, "repetition_penalty")
        putNumberOrRemove(obj, frequencyPenalty, "frequency_penalty")
        putNumberOrRemove(obj, presencePenalty, "presence_penalty")
        putLongOrRemove(obj, seed, "seed")
        putNumberOrRemove(obj, n, "n")
        putNumberOrRemove(obj, maxTokens, "openai_max_tokens")
        putNumberOrRemove(obj, maxContext, "openai_max_context")
        putStringArrayOrRemove(obj, stop, "stop")

        obj.put("stream_openai", streamOutput)
        obj.put("reasoning_effort", reasoningLevel)
        obj.put("names_behavior", characterNamesBehavior)
        obj.put("continue_prefill", continuePrefill)
        obj.put("squash_system_messages", squashSystemMessages)
        obj.put("function_calling", functionCalling)
        obj.put("media_inlining", sendInlineMedia)

        // ── Format fields and utility prompts at root level ──
        obj.put("description_format", descriptionFormat)
        obj.put("persona_format", personaFormat)
        obj.put("personality_format", personalityFormat)
        obj.put("scenario_format", scenarioFormat)
        obj.put("wi_format", wiFormat)
        obj.put("impersonation_prompt", impersonationPrompt)
        obj.put("group_nudge_prompt", groupNudgePrompt)
        obj.put("new_chat_prompt", newChatPrompt)
        obj.put("new_group_chat_prompt", newGroupChatPrompt)
        obj.put("new_example_chat_prompt", newExampleChatPrompt)
        obj.put("continue_nudge_prompt", continueNudgePrompt)
        obj.put("send_if_empty", sendIfEmptyPrompt)

        // ── Extensions: regex scripts ──
        val extensions = obj.optJSONObject("extensions")
        if (regexScripts.isNotEmpty()) {
            val ext = extensions ?: JSONObject()
            ext.put("regex_scripts", JSONArray(regexScripts.map { it.toJson() }))
            ext.put("regex_scripts_enabled", regexScriptsEnabled)
            obj.put("extensions", ext)
        } else {
            val ext = extensions ?: JSONObject()
            ext.remove("regex_scripts")
            ext.put("regex_scripts_enabled", regexScriptsEnabled)
            if (ext.length() == 0) obj.remove("extensions") else obj.put("extensions", ext)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): PresetModelParams {
            return PresetModelParams(
                // Basic sampling — ST keys only, no fallbacks
                temperature = obj.optDoubleOrNull("temperature"),
                topP = obj.optDoubleOrNull("top_p"),
                topK = obj.optDoubleOrNull("top_k"),
                minP = obj.optDoubleOrNull("min_p"),
                topA = obj.optDoubleOrNull("top_a"),
                repetitionPenalty = obj.optDoubleOrNull("repetition_penalty"),
                frequencyPenalty = obj.optDoubleOrNull("frequency_penalty"),
                presencePenalty = obj.optDoubleOrNull("presence_penalty"),
                seed = obj.optLongOrNull("seed"),
                n = obj.optIntOrNull("n"),
                maxTokens = obj.optIntOrNull("openai_max_tokens"),
                maxContext = obj.optIntOrNull("openai_max_context"),
                stop = parseStop(obj),
                // Context & response
                streamOutput = obj.optBoolean("stream_openai", true),
                reasoningLevel = obj.optString("reasoning_effort", "auto"),
                // Naming
                characterNamesBehavior = obj.optInt("names_behavior", 0),
                // Config switches
                continuePrefill = obj.optBoolean("continue_prefill", false),
                squashSystemMessages = obj.optBoolean("squash_system_messages", false),
                functionCalling = obj.optBoolean("function_calling", false),
                sendInlineMedia = obj.optBoolean("media_inlining", false),
                // Format templates — root level (not extensions.*)
                descriptionFormat = obj.optString("description_format", DEFAULT_DESCRIPTION_FORMAT),
                personaFormat = obj.optString("persona_format", DEFAULT_PERSONA_FORMAT),
                personalityFormat = obj.optString("personality_format", DEFAULT_PERSONALITY_FORMAT),
                scenarioFormat = obj.optString("scenario_format", DEFAULT_SCENARIO_FORMAT),
                // Utility prompts — root level
                wiFormat = obj.optString("wi_format", DEFAULT_WI_FORMAT),
                impersonationPrompt = obj.optString("impersonation_prompt", DEFAULT_IMPERSONATION_PROMPT),
                groupNudgePrompt = obj.optString("group_nudge_prompt", DEFAULT_GROUP_NUDGE_PROMPT),
                newChatPrompt = obj.optString("new_chat_prompt", DEFAULT_NEW_CHAT_PROMPT),
                newGroupChatPrompt = obj.optString("new_group_chat_prompt", DEFAULT_NEW_GROUP_CHAT_PROMPT),
                newExampleChatPrompt = obj.optString("new_example_chat_prompt", DEFAULT_NEW_EXAMPLE_CHAT_PROMPT),
                continueNudgePrompt = obj.optString("continue_nudge_prompt", DEFAULT_CONTINUE_NUDGE_PROMPT),
                sendIfEmptyPrompt = obj.optString("send_if_empty", DEFAULT_SEND_IF_EMPTY),

                // Regex scripts
                regexScriptsEnabled = obj.optJSONObject("extensions")
                    ?.optBoolean("regex_scripts_enabled", true) ?: true,
                regexScripts = obj.optJSONObject("extensions")
                    ?.optJSONArray("regex_scripts")
                    ?.let { array ->
                        (0 until array.length()).mapNotNull { i ->
                            RegexScript.fromJson(array.optJSONObject(i))
                        }
                    } ?: emptyList(),
            )
        }

        fun defaults(): PresetModelParams = PresetModelParams()

        // ── Default values (matching ST defaults) ──
        const val DEFAULT_DESCRIPTION_FORMAT = "{{description}}"
        const val DEFAULT_PERSONA_FORMAT = "{{persona}}"
        const val DEFAULT_PERSONALITY_FORMAT = "{{personality}}"
        const val DEFAULT_SCENARIO_FORMAT = "{{scenario}}"
        const val DEFAULT_WI_FORMAT = "{0}"
        const val DEFAULT_IMPERSONATION_PROMPT = "[Write your next reply from the point of view of {{user}}, using the chat history so far as a guideline for the writing style of {{user}}. Don't write as {{char}} or system. Don't describe actions of {{char}}.]"
        const val DEFAULT_GROUP_NUDGE_PROMPT = "[Write the next reply only as {{char}}.]"
        const val DEFAULT_NEW_CHAT_PROMPT = "[Start a new Chat]"
        const val DEFAULT_NEW_GROUP_CHAT_PROMPT = "[Start a new group chat. Group members: {{group}}]"
        const val DEFAULT_NEW_EXAMPLE_CHAT_PROMPT = "[Example Chat]"
        const val DEFAULT_CONTINUE_NUDGE_PROMPT = "[Continue your last message without repeating its original content.]"
        const val DEFAULT_SEND_IF_EMPTY = ""

        // ── JSON helpers ──

        private fun JSONObject.optDoubleOrNull(key: String): Float? {
            return if (has(key)) optDouble(key).toFloat() else null
        }

        private fun JSONObject.optIntOrNull(key: String): Int? {
            return if (has(key)) optInt(key) else null
        }

        private fun JSONObject.optLongOrNull(key: String): Long? {
            return if (has(key)) optLong(key) else null
        }

        private fun parseStop(obj: JSONObject): List<String> {
            obj.optJSONArray("stop")?.let { arr ->
                val result = (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
                if (result.isNotEmpty()) return result
            }
            obj.optString("stop", "").takeIf { it.isNotBlank() }?.let { return listOf(it) }
            return emptyList()
        }
    }
}

// ── Top-level helpers for applyToJson ──

private fun removeOldKeys(obj: JSONObject) {
    // Remove old MR JSON keys that have been renamed to ST keys
    listOf(
        "temp_openai", "top_p_openai", "top_k_openai", "min_p_openai",
        "top_a_openai", "repetition_penalty_openai", "freq_pen_openai",
        "pres_pen_openai", "seed_openai", "n_openai",
        "reasoning_level", "character_names_behavior", "send_inline_media",
    ).forEach { obj.remove(it) }

    // Remove old format/utility fields from extensions (now at root level)
    obj.optJSONObject("extensions")?.let { ext ->
        listOf(
            "description_format", "persona_format", "personality_format",
            "scenario_format", "wi_format", "impersonation_prompt",
            "group_nudge_prompt", "new_chat_prompt", "new_group_chat_prompt",
            "new_example_chat_prompt", "continue_nudge_prompt", "send_if_empty",
        ).forEach { ext.remove(it) }
    }
}

private fun putNumberOrRemove(obj: JSONObject, value: Number?, key: String) {
    if (value == null) obj.remove(key) else obj.put(key, value)
}

private fun putLongOrRemove(obj: JSONObject, value: Long?, key: String) {
    if (value == null) obj.remove(key) else obj.put(key, value)
}

private fun putStringArrayOrRemove(obj: JSONObject, value: List<String>, key: String) {
    obj.remove(key)
    if (value.isNotEmpty()) {
        obj.put(key, JSONArray(value))
    }
}
