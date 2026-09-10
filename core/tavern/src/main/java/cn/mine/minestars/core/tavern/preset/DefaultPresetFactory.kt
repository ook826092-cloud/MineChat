package cn.mine.minestars.core.tavern.preset

import org.json.JSONArray
import org.json.JSONObject

object DefaultPresetFactory {

    const val DEFAULT_PRESET_ID = "00000000-0000-0000-0000-000000000000"

    /** Ordered identifiers for the default prompt_order */
    val DEFAULT_ORDER = listOf(
        "main",
        "worldInfoBefore",
        "personaDescription",
        "charDescription",
        "charPersonality",
        "scenario",
        "enhanceDefinitions",
        "nsfw",
        "worldInfoAfter",
        "dialogueExamples",
        "chatHistory",
        "jailbreak",
    )

    fun createDefaultPresetJson(): String {
        val prompts = JSONArray()

        for (identifier in DEFAULT_ORDER) {
            prompts.put(buildPromptObject(identifier))
        }

        val order = JSONArray()
        for (identifier in DEFAULT_ORDER) {
            order.put(
                JSONObject().apply {
                    put("identifier", identifier)
                    put("enabled", defaultEnabled(identifier))
                }
            )
        }

        val promptOrder = JSONArray().put(
            JSONObject().apply {
                put("character_id", 100000)
                put("order", order)
            }
        )

        val modelParams = PresetModelParams.defaults().toJson()

        return JSONObject().apply {
            put("name", "默认预设")
            put("prompts", prompts)
            put("prompt_order", promptOrder)
            // Merge model params into the root JSON (SillyTavern compatible)
            val keysIt = modelParams.keys()
            while (keysIt.hasNext()) {
                val key = keysIt.next()
                put(key, modelParams.get(key))
            }
        }.toString()
    }

    private fun buildPromptObject(identifier: String): JSONObject {
        val lower = identifier.lowercase()
        return when (lower) {
            "main" -> JSONObject().apply {
                put("identifier", "main")
                put("name", "Main Prompt")
                put("system_prompt", true)
                put("role", "system")
                put("marker", false)
                put("content", "Write {{char}}'s next reply in a fictional chat between {{char}} and {{user}}.")
            }
            "nsfw" -> JSONObject().apply {
                put("identifier", "nsfw")
                put("name", "Auxiliary Prompt")
                put("system_prompt", true)
                put("role", "system")
                put("marker", false)
                put("content", "")
            }
            "dialogueexamples" -> JSONObject().apply {
                put("identifier", "dialogueExamples")
                put("name", "Chat Examples")
                put("system_prompt", true)
                put("marker", true)
            }
            "jailbreak" -> JSONObject().apply {
                put("identifier", "jailbreak")
                put("name", "Post-History Instructions")
                put("system_prompt", true)
                put("role", "system")
                put("marker", false)
                put("content", "")
            }
            "chathistory" -> JSONObject().apply {
                put("identifier", "chatHistory")
                put("name", "Chat History")
                put("system_prompt", true)
                put("marker", true)
            }
            "worldinfoafter" -> JSONObject().apply {
                put("identifier", "worldInfoAfter")
                put("name", "World Info (after)")
                put("system_prompt", true)
                put("marker", true)
            }
            "worldinfobefore" -> JSONObject().apply {
                put("identifier", "worldInfoBefore")
                put("name", "World Info (before)")
                put("system_prompt", true)
                put("marker", true)
            }
            "enhancedefinitions" -> JSONObject().apply {
                put("identifier", "enhanceDefinitions")
                put("name", "Enhance Definitions")
                put("system_prompt", true)
                put("role", "system")
                put("marker", false)
                put("content", "If you have more knowledge of {{char}}, add to the character's lore and personality to enhance them but keep the Character Sheet's definitions absolute.")
            }
            "chardescription" -> JSONObject().apply {
                put("identifier", "charDescription")
                put("name", "Char Description")
                put("system_prompt", true)
                put("marker", true)
            }
            "charpersonality" -> JSONObject().apply {
                put("identifier", "charPersonality")
                put("name", "Char Personality")
                put("system_prompt", true)
                put("marker", true)
            }
            "scenario" -> JSONObject().apply {
                put("identifier", "scenario")
                put("name", "Scenario")
                put("system_prompt", true)
                put("marker", true)
            }
            "personadescription" -> JSONObject().apply {
                put("identifier", "personaDescription")
                put("name", "Persona Description")
                put("system_prompt", true)
                put("marker", true)
            }
            else -> JSONObject().apply {
                put("identifier", identifier)
                put("name", identifier)
                put("system_prompt", true)
            }
        }
    }

    fun extractEntryIdentifiers(rawJson: String): List<String> {
        return try {
            val obj = JSONObject(rawJson)
            val orderArray = obj.optJSONArray("prompt_order") ?: return DEFAULT_ORDER
            if (orderArray.length() == 0) return DEFAULT_ORDER

            val first = orderArray.optJSONObject(0) ?: return DEFAULT_ORDER
            val order = first.optJSONArray("order") ?: return DEFAULT_ORDER
            (0 until order.length()).mapNotNull { index ->
                when (val item = order.opt(index)) {
                    is JSONObject -> item.optString("identifier")
                    is String -> item
                    else -> ""
                }.ifBlank { null }
            }
        } catch (_: Exception) {
            DEFAULT_ORDER
        }
    }

    private fun defaultEnabled(identifier: String): Boolean {
        return identifier != "enhanceDefinitions"
    }
}
