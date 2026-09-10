package cn.mine.minestars.data.tavern.pipeline

import android.util.Log
import cn.mine.minestars.data.model.Assistant
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses SillyTavern-format regex scripts JSON into [TavernRegexScript] list.
 */
object TavernRegexParser {

    /**
     * Parse assistant-level regex scripts from [Assistant.regexScriptsJson].
     */
    fun fromAssistant(assistant: Assistant?): List<TavernRegexScript> {
        if (assistant == null) {
            Log.d("TavernRegex", "fromAssistant: assistant is null → empty")
            return emptyList()
        }
        val scripts = fromJson(assistant.regexScriptsJson)
        Log.d("TavernRegex", "fromAssistant: assistant.id=${assistant.id} parsed ${scripts.size} scripts")
        if (scripts.isNotEmpty()) {
            Log.d("TavernRegex", "  regexScriptsJson=${assistant.regexScriptsJson?.take(300)}")
        }
        return scripts
    }

    /**
     * Parse the embedded JSON format:
     * ```json
     * { "enabled": true, "scripts": [{ "id": "...", "rawJson": "{...}", "enabled": true, "sortOrder": 0 }] }
     * ```
     */
    fun fromJson(regexScriptsJson: String?): List<TavernRegexScript> {
        if (regexScriptsJson.isNullOrBlank()) {
            Log.d("TavernRegex", "fromJson: input is null/blank → empty")
            return emptyList()
        }
        return runCatching {
            val trimmed = regexScriptsJson.trim()
            val arr = if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                val masterEnabled = root.optBoolean("enabled", true)
                if (!masterEnabled) {
                    Log.d("TavernRegex", "fromJson: root enabled=false → empty (master switch OFF)")
                    return@runCatching emptyList<TavernRegexScript>()
                }
                root.optJSONArray("scripts")
            } else {
                JSONArray(trimmed)
            }
            if (arr == null) {
                Log.d("TavernRegex", "fromJson: scripts array is null → empty")
                return@runCatching emptyList<TavernRegexScript>()
            }
            val result = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val scriptEnabled = obj.optBoolean("enabled", true)
                if (!scriptEnabled) {
                    Log.d("TavernRegex", "fromJson: script[$i] enabled=false → skip")
                    return@mapNotNull null
                }
                val rawJson = obj.optString("rawJson", "{}")
                val parsed = parseSingle(rawJson)
                if (parsed == null) {
                    Log.w("TavernRegex", "fromJson: script[$i] parseSingle returned null")
                }
                parsed
            }
            Log.d("TavernRegex", "fromJson: parsed ${result.size} scripts")
            result
        }.getOrDefault(emptyList()).also { result ->
            if (result.isEmpty()) {
                Log.d("TavernRegex", "fromJson: result is empty (parse error or all disabled)")
            }
        }
    }

    /**
     * Parse a single TavernRegexScript from its raw JSON string.
     */
    fun parseSingle(rawJson: String): TavernRegexScript? {
        return try {
            val obj = JSONObject(rawJson)
            val script = TavernRegexScript(
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
                    (0 until arr.length()).mapNotNull {
                        arr.optInt(it).takeIf { value -> value != 0 || !arr.isNull(it) }
                    }
                } ?: emptyList(),
                runOnEdit = obj.optBoolean("runOnEdit", true),
                minDepth = if (obj.has("minDepth")) obj.optInt("minDepth") else null,
                maxDepth = if (obj.has("maxDepth")) obj.optInt("maxDepth") else null,
            )
            Log.d("TavernRegex", "parseSingle: '${script.scriptName}' " +
                    "disabled=${script.disabled} mdOnly=${script.markdownOnly} ppOnly=${script.promptOnly} " +
                    "placement=${script.placement} find='${script.findRegex}' replace='${script.replaceString.take(80)}'")
            script
        } catch (e: Exception) {
            Log.w("TavernRegex", "parseSingle: failed to parse rawJson='${rawJson.take(200)}': ${e.message}")
            null
        }
    }
}
