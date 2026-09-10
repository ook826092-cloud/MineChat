package cn.mine.minestars.core.tavern.worldbook

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object WorldBookParser {
    fun parse(rawJson: String): List<WorldBookEntry> {
        Log.d(WB_TAG, "Start parsing world book JSON, length=${rawJson.length}")
        val root = runCatching { JSONObject(rawJson) }.getOrNull()
        val entrySource = root?.optJSONArray("entries")
            ?: root?.optJSONObject("data")?.optJSONArray("entries")
            ?: root?.optJSONObject("character_book")?.optJSONArray("entries")
            ?: root?.opt("entries")
            ?: runCatching { JSONArray(rawJson) }.getOrNull()
            ?: run {
                Log.w(WB_TAG, "Unable to parse world book entries")
                return emptyList()
            }

        val entries = when (entrySource) {
            is JSONArray -> (0 until entrySource.length()).mapNotNull { index ->
                val node = entrySource.optJSONObject(index) ?: return@mapNotNull null
                parseEntry(node, index)
            }
            is JSONObject -> {
                val keys = entrySource.keys().asSequence().toList()
                keys.sortedWith(compareBy({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it })).mapNotNull { key ->
                    val node = entrySource.optJSONObject(key) ?: return@mapNotNull null
                    parseEntry(node, key.toIntOrNull() ?: 0)
                }
            }
            else -> {
                Log.w(WB_TAG, "Unable to parse world book entries")
                emptyList()
            }
        }

        Log.d(WB_TAG, "Parsed world book entries: ${entries.size}")
        return entries
    }

    private fun parseEntry(node: JSONObject, index: Int): WorldBookEntry? {
        val rawContent = node.optString("content")
        val (decorators, content) = parseDecorators(rawContent)
        val forceActivate = decorators.contains("@@activate")
        val forceDisable = decorators.contains("@@dont_activate")
        val extensions = node.optJSONObject("extensions") ?: JSONObject()
        val title = node.optString("comment").trim().ifBlank {
            node.optString("name").trim()
        }.ifBlank { null }
        val position = resolvePosition(node)
            ?: throw IllegalArgumentException(
                "WorldBook entry missing extensions.position: uid=${node.optIntOrNull("uid") ?: node.optIntOrNull("id")}, title=${title ?: "(no title)"}"
            )

        val order = if (node.has("keys") || node.has("secondary_keys") || node.has("comment") || node.has("constant")) {
            extensions.optIntOrNull("order")
                ?: node.optIntOrNull("order")
                ?: node.optIntOrNull("insertion_order")
                ?: index
        } else {
            node.optIntOrNull("order")
                ?: node.optIntOrNull("insertion_order")
                ?: node.optIntOrNull("uid")
                ?: node.optIntOrNull("id")
                ?: index
        }
        val enabled = when {
            node.has("enabled") -> node.optBoolean("enabled", true)
            node.has("disable") -> !node.optBoolean("disable", false)
            else -> true
        }
        val selectiveLogic = extensions.optIntOrNull("selectiveLogic")
            ?: extensions.optIntOrNull("selective_logic")
            ?: node.optIntOrNull("selectiveLogic")
            ?: 0
        val probability = extensions.optIntOrNull("probability")
        val useProbability = extensions.optBooleanOrNull("useProbability")
            ?: extensions.optBooleanOrNull("use_probability")
            ?: false
        val roleValue = extensions.optIntOrNull("role") ?: 0

        return WorldBookEntry(
            uid = node.optIntOrNull("uid") ?: node.optIntOrNull("id") ?: index,
            title = title,
            content = content,
            decorators = decorators,
            forceActivate = forceActivate,
            forceDisable = forceDisable,
            keys = parseStringList(node.opt("keys"))
                .ifEmpty { parseStringList(node.opt("key")) }
                .ifEmpty { parseStringList(node.opt("keywords")) }
                .ifEmpty { parseStringList(node.opt("primary_keys")) },
            secondaryKeys = parseStringList(node.opt("secondary_keys"))
                .ifEmpty { parseStringList(node.opt("keysecondary")) }
                .ifEmpty { parseStringList(node.opt("filter_keys")) },
            enabled = enabled,
            constant = node.optBoolean("constant", false),
            vectorized = extensions.optBoolean("vectorized", false),
            position = position,
            depth = extensions.optIntOrNull("depth"),
            order = order,
            selective = node.optBoolean("selective", true),
            selectiveLogic = selectiveLogic,
            useRegex = node.optBoolean("use_regex", false),
            scanDepth = extensions.optIntOrNull("scan_depth"),
            probability = probability,
            useProbability = useProbability,
            group = extensions.optString("group").trim(),
            groupOverride = extensions.optBoolean("group_override", false),
            groupWeight = extensions.optIntOrNull("group_weight"),
            sticky = extensions.optIntOrNull("sticky"),
            cooldown = extensions.optIntOrNull("cooldown"),
            delay = extensions.optIntOrNull("delay"),
            caseSensitive = node.optBooleanOrNull("case_sensitive") ?: extensions.optBooleanOrNull("case_sensitive"),
            matchWholeWords = extensions.optBooleanOrNull("match_whole_words"),
            matchPersonaDescription = extensions.optBoolean("match_persona_description", false),
            matchCharacterDescription = extensions.optBoolean("match_character_description", false),
            matchCharacterPersonality = extensions.optBoolean("match_character_personality", false),
            matchCharacterDepthPrompt = extensions.optBoolean("match_character_depth_prompt", false),
            matchScenario = extensions.optBoolean("match_scenario", false),
            matchCreatorNotes = extensions.optBoolean("match_creator_notes", false),
            delayUntilRecursion = extensions.optBoolean("delay_until_recursion", false),
            preventRecursion = extensions.optBoolean("prevent_recursion", false),
            excludeRecursion = extensions.optBoolean("exclude_recursion", false),
            useGroupScoring = extensions.optBooleanOrNull("use_group_scoring"),
            automationId = extensions.optString("automation_id").ifBlank { null },
            role = when (roleValue) {
                1 -> "user"
                2 -> "assistant"
                else -> "system"
            },
            outletName = extensions.optString("outlet_name").ifBlank { null },
            triggers = parseStringList(extensions.opt("triggers"))
                .map { it.lowercase() }
                .ifEmpty { emptyList() },
            ignoreBudget = extensions.optBoolean("ignore_budget", false),
            characterFilterNames = extensions.optJSONObject("character_filter")
                ?.optJSONArray("names")
                ?.let { (0 until it.length()).mapNotNull { idx -> it.optString(idx).trim().takeIf { s -> s.isNotBlank() } } }
                ?: emptyList(),
            characterFilterTags = extensions.optJSONObject("character_filter")
                ?.optJSONArray("tags")
                ?.let { (0 until it.length()).mapNotNull { idx -> it.optString(idx).trim().takeIf { s -> s.isNotBlank() } } }
                ?: emptyList(),
            characterFilterExclude = extensions.optJSONObject("character_filter")
                ?.optBoolean("isExclude", false)
                ?: false
        )
    }

    private fun parseDecorators(content: String): Pair<List<String>, String> {
        if (content.isBlank()) return emptyList<String>() to content.trim()
        val lines = content.lines()
        val decorators = mutableListOf<String>()
        var index = 0
        while (index < lines.size) {
            val line = lines[index].trim()
            if (line.startsWith("@@")) {
                val token = line.split(' ', '\t').firstOrNull().orEmpty()
                if (token == "@@activate" || token == "@@dont_activate") {
                    decorators += token
                    index++
                    continue
                }
            }
            break
        }
        val stripped = lines.drop(index).joinToString("\n").trim()
        return decorators to stripped
    }

    private fun resolvePosition(node: JSONObject): Int? {
        val extensions = node.optJSONObject("extensions") ?: return null
        return extensions.optIntOrNull("position")
    }

    private fun parseStringList(value: Any?): List<String> {
        return when (value) {
            is JSONArray -> (0 until value.length()).mapNotNull { index ->
                value.optString(index).trim().takeIf { it.isNotBlank() }
            }
            is String -> value.split(',', '\n').map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return optBoolean(key)
    }
}
