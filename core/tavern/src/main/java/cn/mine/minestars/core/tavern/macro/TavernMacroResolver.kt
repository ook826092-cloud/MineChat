package cn.mine.minestars.core.tavern.macro

data class TavernMacroContext(
    val userName: String = "user",
    val charName: String = "",
    val group: String = "",
    val groupNotMuted: String = "",
    val notChar: String = "",
    val model: String = "",
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val persona: String = "",
    val mesExamplesRaw: String = "",
    val firstMessage: String = "",
    val alternateGreetings: List<String> = emptyList(),
    val mainPromptOverride: String = "",
    val postHistoryInstructions: String = "",
    val creatorNotes: String = "",
    val characterVersion: String = "",
    val charDepthPrompt: String = "",
    val original: String? = null,
    val isMobile: Boolean = true,
)

object TavernMacroResolver {
    private val macroPattern = Regex("\\{\\{([A-Za-z_][A-Za-z0-9_]*)(?:::(.*?))?\\}\\}")

    fun resolve(
        content: String,
        context: TavernMacroContext,
        maxPasses: Int = 4,
        macroPostProcess: (String) -> String = { it },
    ): String {
        if (content.isBlank() || !content.contains("{{")) return content

        var originalConsumed = false
        var current = content
        repeat(maxPasses.coerceAtLeast(1)) {
            var changed = false
            val next = macroPattern.replace(current) { match ->
                val key = match.groupValues[1]
                val arg = match.groups[2]?.value
                val replacement = resolveMacro(
                    key = key,
                    arg = arg,
                    context = context,
                    originalConsumed = originalConsumed,
                )
                if (key.equals("original", ignoreCase = true)) {
                    originalConsumed = true
                }
                if (replacement != null) {
                    changed = true
                    macroPostProcess(replacement)
                } else {
                    match.value
                }
            }
            current = next
            if (!changed || !current.contains("{{")) return current
        }
        return current
    }

    private fun resolveMacro(
        key: String,
        arg: String?,
        context: TavernMacroContext,
        originalConsumed: Boolean,
    ): String? {
        return when (key.lowercase()) {
            "user" -> context.userName
            "char" -> context.charName
            "group", "charifnotgroup" -> context.group.ifBlank { context.charName }
            "groupnotmuted" -> context.groupNotMuted
            "notchar" -> context.notChar
            "model" -> context.model
            "charprompt" -> context.mainPromptOverride
            "charinstruction", "charjailbreak" -> context.postHistoryInstructions
            "chardescription", "description" -> context.description
            "charpersonality", "personality" -> context.personality
            "charscenario", "scenario" -> context.scenario
            "persona", "personadescription" -> context.persona
            "mesexamplesraw" -> context.mesExamplesRaw
            "mesexamples" -> formatMesExamples(context.mesExamplesRaw)
            "chardepthprompt" -> context.charDepthPrompt
            "charcreatornotes", "creatornotes" -> context.creatorNotes
            "charfirstmessage", "greeting" -> resolveGreeting(context, arg)
            "charversion", "version", "char_version" -> context.characterVersion
            "original" -> if (originalConsumed) "" else context.original.orEmpty()
            "ismobile" -> context.isMobile.toString()
            else -> null
        }
    }

    private fun resolveGreeting(context: TavernMacroContext, arg: String?): String {
        val index = arg?.trim()?.toIntOrNull() ?: 0
        if (index == 0) return context.firstMessage
        return context.alternateGreetings.getOrNull(index - 1).orEmpty()
    }

    private fun formatMesExamples(raw: String): String {
        if (raw.isBlank() || raw == "<START>") return ""
        val normalized = if (raw.startsWith("<START>", ignoreCase = true)) {
            raw
        } else {
            "<START>\n${raw.trim()}"
        }
        return normalized
            .split(Regex("<START>", RegexOption.IGNORE_CASE))
            .drop(1)
            .mapNotNull { block ->
                block.trim().takeIf { it.isNotBlank() }?.let { "<START>\n$it\n" }
            }
            .joinToString("")
    }
}
