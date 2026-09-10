package cn.mine.minestars.core.tavern.macro

object TemplateMacroEngine {
    private val macroRegex = Regex("\\{\\{\\s*([^{}]+?)\\s*\\}\\}")
    private data class ScopedFrame(
        val type: String,
        val start: Int,
        val startEnd: Int,
        val condition: String = "",
        var elseStart: Int? = null,
        var elseEnd: Int? = null
    )

    fun resolve(
        text: String,
        context: TemplateMacroContext,
        maxRounds: Int = 4
    ): String {
        if (text.isBlank()) return text
        var current = text
        repeat(maxRounds) {
            val scopedResolved = resolveScoped(current, context)
            val next = resolveInline(scopedResolved, context)
            if (next == current) return@repeat
            current = next
        }
        return current.trim()
    }

    fun stripUnknown(text: String): String {
        if (text.isBlank()) return text
        return buildString {
            TemplateMacroParser.parse(text).forEach { token ->
                when (token) {
                    is TemplateMacroToken.Text -> append(token.value)
                    is TemplateMacroToken.Macro -> {
                        if (!TemplateMacroExecutor.isSupported(token.name)) {
                            return@forEach
                        }
                        append(token.raw)
                    }
                }
            }
        }.replace(Regex("\\{\\{[^{}]*\\}\\}"), "").trim()
    }

    private fun resolveInline(text: String, context: TemplateMacroContext): String {
        return buildString {
            TemplateMacroParser.parse(text).forEach { token ->
                when (token) {
                    is TemplateMacroToken.Text -> append(token.value)
                    is TemplateMacroToken.Macro -> {
                        append(TemplateMacroExecutor.execute(token, context) ?: "")
                    }
                }
            }
        }
    }

    private fun resolveScoped(text: String, context: TemplateMacroContext): String {
        var current = text
        while (true) {
            val resolved = replaceInnermostScopedBlock(current, context)
            if (resolved == current) return resolved
            current = resolved
        }
    }

    private fun replaceInnermostScopedBlock(text: String, context: TemplateMacroContext): String {
        val stack = mutableListOf<ScopedFrame>()
        macroRegex.findAll(text).forEach { match ->
            val raw = match.groupValues[1].trim()
            when {
                raw.equals("else", ignoreCase = true) -> {
                    val frame = stack.lastOrNull()
                    if (frame?.type == "if" && frame.elseStart == null) {
                        frame.elseStart = match.range.first
                        frame.elseEnd = match.range.last + 1
                    }
                }
                raw.equals("/if", ignoreCase = true) -> {
                    val frameIndex = stack.indexOfLast { it.type == "if" }
                    if (frameIndex >= 0) {
                        val frame = stack.removeAt(frameIndex)
                        return replaceIfBlock(text, frame, match.range.first, match.range.last + 1, context)
                    }
                }
                raw.equals("///", ignoreCase = true) -> {
                    val frameIndex = stack.indexOfLast { it.type == "comment" }
                    if (frameIndex >= 0) {
                        val frame = stack.removeAt(frameIndex)
                        return text.replaceRange(frame.start, match.range.last + 1, "")
                    }
                }
                raw.equals("//", ignoreCase = true) -> {
                    stack += ScopedFrame(
                        type = "comment",
                        start = match.range.first,
                        startEnd = match.range.last + 1
                    )
                }
                isIfOpen(raw) -> {
                    stack += ScopedFrame(
                        type = "if",
                        start = match.range.first,
                        startEnd = match.range.last + 1,
                        condition = parseIfCondition(raw)
                    )
                }
            }
        }
        return text
    }

    private fun replaceIfBlock(
        text: String,
        frame: ScopedFrame,
        closeStart: Int,
        closeEnd: Int,
        context: TemplateMacroContext
    ): String {
        val thenContent = if (frame.elseStart == null) {
            text.substring(frame.startEnd, closeStart)
        } else {
            text.substring(frame.startEnd, frame.elseStart ?: closeStart)
        }
        val elseContent = if (frame.elseStart == null || frame.elseEnd == null) {
            null
        } else {
            text.substring(frame.elseEnd ?: closeStart, closeStart)
        }
        val chosen = if (evaluateCondition(frame.condition, context)) thenContent else elseContent.orEmpty()
        val resolvedContent = resolve(chosen, context, maxRounds = 2)
        return text.replaceRange(frame.start, closeEnd, resolvedContent)
    }

    private fun evaluateCondition(condition: String, context: TemplateMacroContext): Boolean {
        val trimmed = condition.trim()
        if (trimmed.isBlank()) return false
        val inverted = trimmed.startsWith("!")
        val rawValue = if (inverted) trimmed.removePrefix("!").trim() else trimmed
        val resolved = when {
            rawValue.startsWith(".") -> context.read(rawValue.removePrefix("."))
            rawValue.startsWith("$") -> context.readGlobal(rawValue.removePrefix("$"))
            else -> resolveInline(resolveScoped(rawValue, context), context).trim()
        }
        val result = TemplateMacroExecutor.isTruthy(resolved)
        return if (inverted) !result else result
    }

    private fun isIfOpen(raw: String): Boolean {
        return raw.startsWith("if::", ignoreCase = true) || raw.startsWith("if ", ignoreCase = true)
    }

    private fun parseIfCondition(raw: String): String {
        return raw
            .removePrefix("if")
            .trim()
            .removePrefix("::")
            .trim()
    }

    fun resolveWithContextKey(
        text: String,
        context: TemplateMacroContext,
        scopeKey: String,
        maxRounds: Int = 4
    ): String {
        val scopedContext = context.copy(variables = TemplateMacroVariableStore.assistantScope(scopeKey, context.variables))
        return resolve(text, scopedContext, maxRounds)
    }

    fun stripUnknownWithContextKey(text: String, scopeKey: String): String {
        val scopedContext = TemplateMacroContext(
            variables = TemplateMacroVariableStore.assistantScope(scopeKey),
            globalVariables = TemplateMacroVariableStore.globalScope(),
            builtins = emptyMap()
        )
        return stripUnknown(resolveInline(resolveScoped(text, scopedContext), scopedContext))
    }
}
