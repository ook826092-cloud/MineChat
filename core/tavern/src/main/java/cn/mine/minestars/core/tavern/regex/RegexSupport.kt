package cn.mine.minestars.core.tavern.regex

import org.json.JSONArray
import org.json.JSONObject

enum class RegexSource {
    USER_INPUT,
    AI_OUTPUT,
    SLASH_COMMAND,
    WORLD_INFO,
    REASONING
}

enum class RegexDestination {
    DISPLAY,
    PROMPT
}

data class RegexScript(
    val id: String = "",
    val scriptName: String,
    val findRegex: String,
    val replaceString: String,
    val trimStrings: List<String>,
    val disabled: Boolean,
    val promptOnly: Boolean,
    val substituteRegex: Int,
    val minDepth: Int?,
    val maxDepth: Int?,
    val runOnEdit: Boolean,
    val sources: Set<RegexSource>,
    val destinations: Set<RegexDestination>,
    val placement: List<Int>,
    val origin: String = "module"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            if (id.isNotBlank()) put("id", id)
            put("scriptName", scriptName)
            put("findRegex", findRegex)
            put("replaceString", replaceString)
            put("disabled", disabled)
            put("promptOnly", promptOnly)
            put("substituteRegex", substituteRegex)
            minDepth?.let { put("minDepth", it) }
            maxDepth?.let { put("maxDepth", it) }
            put("runOnEdit", runOnEdit)
            if (trimStrings.isNotEmpty()) {
                put("trimStrings", JSONArray(trimStrings))
            }
            if (placement.isNotEmpty()) {
                put("placement", JSONArray(placement))
            }
            // sources → placement-compatible JSON (ST format uses placement array)
            // destinations handled via promptOnly/markdownOnly in ST format
        }
    }

    companion object {
        fun fromJson(node: JSONObject): RegexScript? {
            return ChatRequestRegexSupport.parseRegexScriptNode(node)
        }
    }
}

object ChatRequestRegexSupport {
    fun parseRegexScripts(rawJson: String): List<RegexScript> {
        val root = runCatching { JSONObject(rawJson) }.getOrNull()
        val array = when {
            root?.has("regex_scripts") == true -> root.optJSONArray("regex_scripts")
            root != null -> JSONArray().put(root)
            else -> runCatching { JSONArray(rawJson) }.getOrNull()
        } ?: return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            val node = array.optJSONObject(index) ?: return@mapNotNull null
            parseRegexScriptNode(node)
        }
    }

    fun parsePresetRegexScripts(rawJson: String): List<RegexScript> {
        val root = runCatching { JSONObject(rawJson) }.getOrNull() ?: return emptyList()
        val direct = root.optJSONObject("extensions")?.optJSONArray("regex_scripts")
        val binding = root.optJSONObject("extensions")
            ?.optJSONObject("SPreset")
            ?.optJSONObject("RegexBinding")
            ?.optJSONArray("regexes")
        val merged = mutableListOf<RegexScript>()
        listOfNotNull(direct, binding).forEach { array ->
            for (index in 0 until array.length()) {
                val node = array.optJSONObject(index) ?: continue
                parseRegexScriptNode(node)?.copy(origin = "preset")?.let(merged::add)
            }
        }
        return merged
            .distinctBy { listOf(it.scriptName, it.findRegex, it.replaceString, it.placement.joinToString(","), it.origin).joinToString("|") }
            .filterNot { it.disabled }
    }

    fun applyRegexScripts(
        text: String,
        scripts: List<RegexScript>,
        depthFromLatest: Int,
        source: RegexSource,
        destination: RegexDestination
    ): String {
        if (text.isBlank() || scripts.isEmpty()) return text
        var result = text
        scripts.forEach { script ->
            if (script.disabled) return@forEach
            if (script.sources.isNotEmpty() && source !in script.sources) return@forEach
            if (script.destinations.isNotEmpty() && destination !in script.destinations) return@forEach
            if (script.minDepth != null && depthFromLatest < script.minDepth) return@forEach
            if (script.maxDepth != null && depthFromLatest > script.maxDepth) return@forEach
            val compiled = compileTavernRegex(script.findRegex)
                ?: return@forEach
            result = runCatching { compiled.replace(result, script.replaceString) }.getOrDefault(result)
            script.trimStrings.forEach { trim ->
                if (trim.isNotEmpty()) result = result.replace(trim, "")
            }
        }
        return result
    }

    fun parseRegexScriptNode(node: JSONObject): RegexScript? {
        val findRegex = node.optString("findRegex").ifBlank { node.optString("find_regex") }
            .ifBlank { node.optString("pattern") }
            .trim()
        if (findRegex.isBlank()) return null
        val placement = node.optJSONArray("placement").toIntList()
        val sourceObj = node.optJSONObject("source")
        val destinationObj = node.optJSONObject("destination")
        val sources = buildSet {
            if (sourceObj?.optBoolean("user_input") == true || 1 in placement) add(RegexSource.USER_INPUT)
            if (sourceObj?.optBoolean("ai_output") == true || 2 in placement) add(RegexSource.AI_OUTPUT)
            if (sourceObj?.optBoolean("world_info") == true || 5 in placement) add(RegexSource.WORLD_INFO)
            if (6 in placement) add(RegexSource.REASONING)
            if (3 in placement) add(RegexSource.SLASH_COMMAND)
        }.ifEmpty { setOf(RegexSource.USER_INPUT, RegexSource.AI_OUTPUT) }

        val promptOnly = node.optBoolean("promptOnly", false)
        val displayOnly = node.optBoolean("only_format_display", false) || node.optBoolean("markdownOnly", false)
        val promptFormatOnly = node.optBoolean("only_format_prompt", false)
        val destinations = buildSet {
            if (destinationObj?.optBoolean("display") == true) add(RegexDestination.DISPLAY)
            if (destinationObj?.optBoolean("prompt") == true) add(RegexDestination.PROMPT)
            if (displayOnly) add(RegexDestination.DISPLAY)
            if (promptFormatOnly || promptOnly) add(RegexDestination.PROMPT)
        }.ifEmpty { setOf(RegexDestination.DISPLAY, RegexDestination.PROMPT) }

        return RegexScript(
            id = node.optString("id", ""),
            scriptName = node.optString("scriptName").ifBlank { node.optString("name") },
            findRegex = findRegex,
            replaceString = node.optString("replaceString")
                .ifBlank { node.optString("replace_string") }
                .ifBlank { node.optString("replacement") }
                .ifBlank { node.optString("replace") },
            trimStrings = node.optJSONArray("trimStrings").toStringList()
                .ifEmpty { node.optJSONArray("trim_strings").toStringList() },
            disabled = node.optBoolean("disabled", false),
            promptOnly = promptOnly,
            substituteRegex = node.optInt("substituteRegex", node.optInt("substitute_regex", 0)),
            minDepth = node.optIntOrNull("minDepth") ?: node.optIntOrNull("min_depth"),
            maxDepth = node.optIntOrNull("maxDepth") ?: node.optIntOrNull("max_depth"),
            runOnEdit = node.optBoolean("runOnEdit", node.optBoolean("run_on_edit", true)),
            sources = sources,
            destinations = destinations,
            placement = placement,
            origin = "module"
        )
    }

    private fun compileTavernRegex(raw: String): Regex? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("/") && trimmed.length >= 2) {
            val lastSlash = trimmed.lastIndexOf('/')
            if (lastSlash > 0) {
                val pattern = trimmed.substring(1, lastSlash)
                val flags = trimmed.substring(lastSlash + 1)
                val options = mutableSetOf<RegexOption>()
                if ('i' in flags) options += RegexOption.IGNORE_CASE
                if ('m' in flags) options += RegexOption.MULTILINE
                val finalPattern = if ('s' in flags) "(?s)$pattern" else pattern
                return runCatching { Regex(finalPattern, options) }.getOrNull()
            }
        }
        return runCatching { Regex(trimmed, setOf(RegexOption.MULTILINE)) }.getOrNull()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optString(index).trim().takeIf { it.isNotBlank() }
        }
    }

    private fun JSONArray?.toIntList(): List<Int> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optInt(index).takeIf { it != 0 || !isNull(index) }
        }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }
}
