package cn.mine.minestars.core.tavern.macro

object TemplateMacroParser {
    private val macroRegex = Regex("\\{\\{\\s*([^{}]+?)\\s*\\}\\}")

    fun parse(text: String): List<TemplateMacroToken> {
        if (text.isEmpty()) return listOf(TemplateMacroToken.Text(""))
        val tokens = mutableListOf<TemplateMacroToken>()
        var cursor = 0
        macroRegex.findAll(text).forEach { match ->
            if (match.range.first > cursor) {
                tokens += TemplateMacroToken.Text(text.substring(cursor, match.range.first))
            }
            tokens += parseMacro(match.value, match.groupValues[1])
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            tokens += TemplateMacroToken.Text(text.substring(cursor))
        }
        return tokens
    }

    private fun parseMacro(raw: String, content: String): TemplateMacroToken {
        val trimmed = content.trim()
        val parts = trimmed.split("::")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size >= 2) {
            val name = parts.first().removePrefix("$").lowercase()
            return TemplateMacroToken.Macro(
                raw = raw,
                name = name,
                args = parts.drop(1)
            )
        }
        // Single-colon syntax for ST compatibility: {{reverse:str}} → name="reverse", args=["str"]
        val scParts = trimmed.split(":", limit = 2)
        if (scParts.size >= 2 && scParts[1].isNotBlank()) {
            val name = scParts.first().trim().removePrefix("$").lowercase()
            if (TemplateMacroExecutor.isSupported(name) || TemplateMacroBuiltinSupport.isBuiltin(name)) {
                return TemplateMacroToken.Macro(
                    raw = raw,
                    name = name,
                    args = listOf(scParts[1].trim())
                )
            }
        }
        val fallbackParts = trimmed.split(Regex("\\s+"), limit = 2)
        if (fallbackParts.size >= 2) {
            return TemplateMacroToken.Macro(
                raw = raw,
                name = fallbackParts.first().removePrefix("$").lowercase(),
                args = listOf(fallbackParts[1].trim())
            )
        }
        val singleName = trimmed.removePrefix("$").lowercase()
        if (TemplateMacroExecutor.isSupported(singleName) || TemplateMacroBuiltinSupport.isBuiltin(singleName)) {
            return TemplateMacroToken.Macro(
                raw = raw,
                name = singleName,
                args = emptyList()
            )
        }
        return TemplateMacroToken.Text(raw)
    }
}
