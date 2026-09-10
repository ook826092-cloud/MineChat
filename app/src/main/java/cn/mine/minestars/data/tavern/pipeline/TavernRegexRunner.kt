package cn.mine.minestars.data.tavern.pipeline

import android.util.Log
import cn.mine.minestars.core.tavern.macro.TavernMacroContext
import cn.mine.minestars.core.tavern.macro.TavernMacroResolver

enum class TavernRegexPlacement(val value: Int) {
    USER_INPUT(1),
    AI_OUTPUT(2),
    WORLD_INFO(5),
    REASONING(6),
}

object TavernRegexRunner {
    private const val TAG = "TavernRegex"

    fun apply(
        text: String,
        scripts: List<TavernRegexScript>,
        placement: TavernRegexPlacement,
        macroContext: TavernMacroContext?,
        isPrompt: Boolean = false,
        isMarkdown: Boolean = false,
        depth: Int? = null,
    ): String {
        if (text.isBlank() || scripts.isEmpty()) {
            return text
        }
        var result = text
        for (script in scripts) {
            if (!script.appliesTo(placement, isPrompt, isMarkdown, depth)) continue
            result = applyScript(result, script, macroContext)
        }
        return result
    }

    fun applyScript(
        text: String,
        script: TavernRegexScript,
        macroContext: TavernMacroContext?,
    ): String {
        if (text.isBlank() || script.disabled || script.findRegex.isBlank()) {
            return text
        }
        val resolvedFind = resolveFindRegex(script, macroContext)
        val regex = compileRegex(resolvedFind)
        if (regex == null) {
            Log.w(TAG, "applyScript: regex failed for '${script.scriptName}': '$resolvedFind'")
            return text
        }
        return runCatching {
            val result = regex.replace(text) { match ->
                val replacement = replaceCaptureGroups(
                    replacement = script.replaceString.replace("{{match}}", "$0", ignoreCase = true),
                    match = match,
                    script = script,
                    macroContext = macroContext,
                )
                if (macroContext == null) replacement
                else TavernMacroResolver.resolve(replacement, macroContext)
            }
            if (result != text) {
                Log.d(TAG, "applyScript: '${script.scriptName}' changed (${text.length}→${result.length})")
            }
            result
        }.onFailure { e ->
            Log.w(TAG, "applyScript: '${script.scriptName}' error: ${e.message}", e)
        }.getOrDefault(text)
    }

    private fun TavernRegexScript.appliesTo(
        placement: TavernRegexPlacement,
        isPrompt: Boolean,
        isMarkdown: Boolean,
        depth: Int?,
    ): Boolean {
        if (disabled) return false
        val placements = placementValues()
        if (placements.isNotEmpty() && placement.value !in placements) return false
        if (markdownOnly) return isMarkdown
        if (promptOnly) return isPrompt
        // Default scripts: run only in normal (send/receive) context
        if (isMarkdown || isPrompt) return false
        if (depth != null) {
            if (minDepth != null && minDepth >= -1 && depth < minDepth) return false
            if (maxDepth != null && maxDepth >= 0 && depth > maxDepth) return false
        }
        return true
    }

    private fun resolveFindRegex(
        script: TavernRegexScript,
        macroContext: TavernMacroContext?,
    ): String {
        if (macroContext == null) return script.findRegex
        return when (script.substituteRegex) {
            1 -> TavernMacroResolver.resolve(script.findRegex, macroContext)
            2 -> TavernMacroResolver.resolve(script.findRegex, macroContext) { it.escapeForRegex() }
            else -> script.findRegex
        }
    }

    private fun replaceCaptureGroups(
        replacement: String,
        match: MatchResult,
        script: TavernRegexScript,
        macroContext: TavernMacroContext?,
    ): String {
        val capturePattern = Regex("""\$(\d+)|\$<([^>]+)>""")
        return capturePattern.replace(replacement) { groupMatch ->
            val value = when {
                groupMatch.groupValues[1].isNotEmpty() -> {
                    val index = groupMatch.groupValues[1].toIntOrNull()
                    if (index != null && index in 0 until match.groups.size) {
                        match.groups[index]?.value.orEmpty()
                    } else {
                        ""
                    }
                }
                groupMatch.groupValues[2].isNotEmpty() -> {
                    match.groups[groupMatch.groupValues[2]]?.value.orEmpty()
                }
                else -> ""
            }
            filterTrimStrings(value, script.trimStrings, macroContext)
        }
    }

    private fun filterTrimStrings(
        value: String,
        trimStrings: List<String>,
        macroContext: TavernMacroContext?,
    ): String {
        if (trimStrings.isEmpty()) return value
        return trimStrings.fold(value) { acc, trim ->
            val resolvedTrim = if (macroContext == null) trim else TavernMacroResolver.resolve(trim, macroContext)
            acc.replace(resolvedTrim, "")
        }
    }

    private fun compileRegex(raw: String): Regex? {
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

    private fun TavernRegexScript.placementValues(): List<Int> {
        return placement
    }

    private fun String.escapeForRegex(): String {
        return buildString {
            for (char in this@escapeForRegex) {
                append(
                    when (char) {
                        '\n' -> "\\n"
                        '\r' -> "\\r"
                        '\t' -> "\\t"
                        '\u000B' -> "\\v"
                        '\u000C' -> "\\f"
                        '\u0000' -> "\\0"
                        '.', '^', '$', '*', '+', '?', '{', '}', '[', ']', '\\', '/', '|', '(', ')' -> "\\$char"
                        else -> char.toString()
                    }
                )
            }
        }
    }
}
