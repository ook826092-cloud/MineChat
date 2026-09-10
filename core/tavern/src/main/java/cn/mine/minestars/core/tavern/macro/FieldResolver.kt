package cn.mine.minestars.core.tavern.macro

object TemplateFieldResolver {
    private val placeholderRegex = Regex("\\{\\{\\s*([^{}]+?)\\s*\\}\\}")

    fun resolve(text: String, fields: TemplateFieldBag): String {
        if (text.isBlank()) return text
        val values = fields.toMap()
        return placeholderRegex.replace(text) { match ->
            val rawKey = match.groupValues[1].trim()
            if (rawKey.contains("::")) {
                match.value
            } else {
                values[rawKey.lowercase()] ?: match.value
            }
        }
    }
}
