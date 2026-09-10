package cn.mine.minestars.core.tavern.preset

import org.json.JSONArray
import org.json.JSONObject

data class PresetEntryData(
    val id: String,
    val identifier: String,
    val name: String,
    val enabled: Boolean,
    val role: String,
    val content: String,
    val injectionPosition: Int?,
    val injectionDepth: Int?,
    val injectionOrder: Int?,
    val systemPrompt: Boolean,
    val marker: Boolean,
    val forbidOverrides: Boolean,
    val injectionTrigger: List<String>,
    val mounted: Boolean
)

object PresetParser {
    fun extractEntries(preset: JSONObject): List<PresetEntryData> {
        val prompts = preset.optJSONArray("prompts")
        if (prompts != null && prompts.length() > 0) {
            val promptsById = linkedMapOf<String, JSONObject>()
            for (i in 0 until prompts.length()) {
                val prompt = prompts.optJSONObject(i) ?: continue
                val id = prompt.optString("identifier")
                if (id.isNotBlank() && !promptsById.containsKey(id)) {
                    promptsById[id] = prompt
                }
            }

            val selectedOrderArray = run {
                val promptOrder = preset.optJSONArray("prompt_order") ?: return@run null
                if (promptOrder.length() == 0) return@run null
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
            }

            val promptEntries = mutableListOf<PresetEntryData>()
            val mountedIds = mutableSetOf<String>()
            if (selectedOrderArray != null) {
                val orderEnabled = mutableMapOf<String, Boolean>()
                val orderIds = mutableListOf<String>()
                for (index in 0 until selectedOrderArray.length()) {
                    val orderItem = selectedOrderArray.opt(index)
                    val identifier = when (orderItem) {
                        is JSONObject -> orderItem.optString("identifier")
                        is String -> orderItem
                        else -> ""
                    }.ifBlank { continue }
                    orderIds += identifier
                    val enabled = if (orderItem is JSONObject && orderItem.has("enabled")) {
                        orderItem.optBoolean("enabled", true)
                    } else {
                        true
                    }
                    orderEnabled[identifier] = enabled
                }

                orderIds.forEachIndexed { index, identifier ->
                    val prompt = promptsById[identifier]
                        ?: builtinPrompt(identifier)?.also { promptsById[identifier] = it }
                        ?: return@forEachIndexed
                    mountedIds += identifier
                    val enabled = orderEnabled[identifier] ?: if (prompt.has("enabled")) {
                        prompt.optBoolean("enabled", true)
                    } else {
                        true
                    }
                    val name = prompt.optString("name").ifBlank { "Item ${index + 1}" }
                    val triggerArray = prompt.optJSONArray("injection_trigger")
                    val triggers = if (triggerArray == null) {
                        emptyList()
                    } else {
                        (0 until triggerArray.length()).mapNotNull { t -> triggerArray.optString(t).takeIf { it.isNotBlank() } }
                    }

                    promptEntries += PresetEntryData(
                        id = "prompt::$identifier::$index",
                        identifier = identifier,
                        name = name,
                        enabled = enabled,
                        role = prompt.optString("role"),
                        content = prompt.optString("content"),
                        injectionPosition = if (prompt.has("injection_position")) prompt.optInt("injection_position") else null,
                        injectionDepth = if (prompt.has("injection_depth")) prompt.optInt("injection_depth") else null,
                        injectionOrder = if (prompt.has("injection_order")) prompt.optInt("injection_order") else null,
                        systemPrompt = prompt.optBoolean("system_prompt", false),
                        marker = prompt.optBoolean("marker", false),
                        forbidOverrides = prompt.optBoolean("forbid_overrides", false),
                        injectionTrigger = triggers,
                        mounted = true
                    )
                }
                val remaining = promptsById.entries.filter { (id, _) -> id !in mountedIds }
                if (remaining.isNotEmpty()) {
                    remaining.forEachIndexed { index, (identifier, prompt) ->
                        val name = prompt.optString("name").ifBlank { "Item ${index + 1}" }
                        val enabled = if (prompt.has("enabled")) prompt.optBoolean("enabled", true) else true
                        val triggerArray = prompt.optJSONArray("injection_trigger")
                        val triggers = if (triggerArray == null) {
                            emptyList()
                        } else {
                            (0 until triggerArray.length()).mapNotNull { t -> triggerArray.optString(t).takeIf { it.isNotBlank() } }
                        }

                        promptEntries += PresetEntryData(
                            id = "prompt::$identifier::unmounted::$index",
                            identifier = identifier,
                            name = name,
                            enabled = enabled,
                            role = prompt.optString("role"),
                            content = prompt.optString("content"),
                            injectionPosition = if (prompt.has("injection_position")) prompt.optInt("injection_position") else null,
                            injectionDepth = if (prompt.has("injection_depth")) prompt.optInt("injection_depth") else null,
                            injectionOrder = if (prompt.has("injection_order")) prompt.optInt("injection_order") else null,
                            systemPrompt = prompt.optBoolean("system_prompt", false),
                            marker = prompt.optBoolean("marker", false),
                            forbidOverrides = prompt.optBoolean("forbid_overrides", false),
                            injectionTrigger = triggers,
                            mounted = false
                        )
                    }
                }
            } else {
                promptsById.entries.forEachIndexed { index, (identifier, prompt) ->
                    val name = prompt.optString("name").ifBlank { "Item ${index + 1}" }
                    val enabled = prompt.optBoolean("enabled", true)
                    val triggerArray = prompt.optJSONArray("injection_trigger")
                    val triggers = if (triggerArray == null) {
                        emptyList()
                    } else {
                        (0 until triggerArray.length()).mapNotNull { t -> triggerArray.optString(t).takeIf { it.isNotBlank() } }
                    }

                    promptEntries += PresetEntryData(
                        id = "prompt::$identifier::$index",
                        identifier = identifier,
                        name = name,
                        enabled = enabled,
                        role = prompt.optString("role"),
                        content = prompt.optString("content"),
                        injectionPosition = if (prompt.has("injection_position")) prompt.optInt("injection_position") else null,
                        injectionDepth = if (prompt.has("injection_depth")) prompt.optInt("injection_depth") else null,
                        injectionOrder = if (prompt.has("injection_order")) prompt.optInt("injection_order") else null,
                        systemPrompt = prompt.optBoolean("system_prompt", false),
                        marker = prompt.optBoolean("marker", false),
                        forbidOverrides = prompt.optBoolean("forbid_overrides", false),
                        injectionTrigger = triggers,
                        mounted = true
                    )
                }
            }

            if (promptEntries.isNotEmpty()) return promptEntries
        }

        val entries = mutableListOf<PresetEntryData>()
        val keys = preset.keys().asSequence().toList().sorted()
        keys.forEach { key ->
            val value = preset.opt(key)
            entries += PresetEntryData(
                id = "field::$key",
                identifier = key,
                name = key,
                enabled = true,
                role = "",
                content = presetValueToDisplay(value),
                injectionPosition = null,
                injectionDepth = null,
                injectionOrder = null,
                systemPrompt = false,
                marker = false,
                forbidOverrides = false,
                injectionTrigger = emptyList(),
                mounted = true
            )
        }
        return entries
    }

    private fun presetValueToDisplay(value: Any?): String {
        return when (value) {
            null -> "null"
            is JSONObject -> value.toString(2)
            is JSONArray -> value.toString(2)
            else -> value.toString()
        }
    }

    private fun builtinPrompt(identifier: String): JSONObject? {
        val key = identifier.lowercase()
        return when (key) {
            "main" -> baseBuiltinPrompt(
                identifier = "main",
                name = "Main Prompt",
                marker = false,
                role = "system",
                content = "Write {{char}}'s next reply in a fictional chat between {{char}} and {{user}}."
            )
            "nsfw" -> baseBuiltinPrompt(
                identifier = "nsfw",
                name = "Auxiliary Prompt",
                marker = false,
                role = "system",
                content = ""
            )
            "dialogueexamples" -> baseBuiltinPrompt(
                identifier = "dialogueExamples",
                name = "Chat Examples",
                marker = true
            )
            "jailbreak" -> baseBuiltinPrompt(
                identifier = "jailbreak",
                name = "Post-History Instructions",
                marker = false,
                role = "system",
                content = ""
            )
            "chathistory" -> baseBuiltinPrompt(
                identifier = "chatHistory",
                name = "Chat History",
                marker = true
            )
            "worldinfoafter" -> baseBuiltinPrompt(
                identifier = "worldInfoAfter",
                name = "World Info (after)",
                marker = true
            )
            "worldinfobefore" -> baseBuiltinPrompt(
                identifier = "worldInfoBefore",
                name = "World Info (before)",
                marker = true
            )
            "enhancedefinitions" -> baseBuiltinPrompt(
                identifier = "enhanceDefinitions",
                name = "Enhance Definitions",
                marker = false,
                role = "system",
                content = "If you have more knowledge of {{char}}, add to the character's lore and personality to enhance them but keep the Character Sheet's definitions absolute."
            )
            "chardescription" -> baseBuiltinPrompt(
                identifier = "charDescription",
                name = "Char Description",
                marker = true
            )
            "charpersonality" -> baseBuiltinPrompt(
                identifier = "charPersonality",
                name = "Char Personality",
                marker = true
            )
            "scenario" -> baseBuiltinPrompt(
                identifier = "scenario",
                name = "Scenario",
                marker = true
            )
            "personadescription" -> baseBuiltinPrompt(
                identifier = "personaDescription",
                name = "Persona Description",
                marker = true
            )
            else -> null
        }
    }

    private fun baseBuiltinPrompt(
        identifier: String,
        name: String,
        marker: Boolean,
        role: String? = null,
        content: String? = null
    ): JSONObject {
        return JSONObject().apply {
            put("identifier", identifier)
            put("name", name)
            put("marker", marker)
            put("system_prompt", true)
            if (!role.isNullOrBlank()) put("role", role)
            if (content != null) put("content", content)
        }
    }
}
