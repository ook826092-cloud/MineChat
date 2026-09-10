package cn.mine.minestars.core.tavern.macro

sealed interface TemplateMacroToken {
    data class Text(val value: String) : TemplateMacroToken

    data class Macro(
        val raw: String,
        val name: String,
        val args: List<String>
    ) : TemplateMacroToken
}
