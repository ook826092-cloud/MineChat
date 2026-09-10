package cn.mine.minestars.core.tavern.macro

object TemplateMacroExecutor {
    /**
     * Supported macro names.
     *
     * MR extensions (macros not present in SillyTavern's original macro system):
     *   var, globalvar, br, lower, upper, capitalize, replace, substring,
     *   default, empty, space, lastUserMessageId, lastCharMessageId,
     *   system, systemprompt, main, notchar, charinstruction, charcreatornotes, charversion
     */
    private val supportedMacros = setOf(
        "setvar", "getvar", "hasvar", "deletevar", "addvar", "incvar", "decvar", "var",
        "setglobalvar", "getglobalvar", "hasglobalvar", "deleteglobalvar", "addglobalvar", "incglobalvar", "decglobalvar", "globalvar",
        "space", "newline", "br", "noop", "trim", "default", "empty", "reverse",
        "lower", "upper", "capitalize", "replace", "substring",
        "if", "random", "pick", "roll",
        "time", "date", "datetime", "weekday", "timestamp",
        "input", "original", "model", "maxprompt", "ismobile",
        "lastmessage", "lastmessageid", "lastusermessage", "lastcharmessage",
        "lastusermessageid", "lastcharmessageid",
        "firstincludedmessageid", "firstdisplayedmessageid", "lastswipeid", "currentswipeid", "lastgenerationtype",
        "charifnotgroup", "group", "groupnotmuted", "notchar",
        "charprompt", "charinstruction", "chardescription", "charpersonality", "charscenario",
        "persona", "personadescription", "personadescriptionraw", "user_description",
        "mesexamplesraw", "mesexamples", "chardepthprompt", "charcreatornotes", "charversion",
        "systemprompt", "system", "main",
        "hasextension", "banned", "outlet",
        // SillyTavern-compatible macros
        "idleduration", "idle_duration",
        "maxcontext", "maxcontexttokens",
        "maxresponse", "maxresponsetokens",
        "allchatrange",
        "timeutc", "time_utc",
        "isotime", "isodate", "datetimeformat", "timediff",
    )

    fun isSupported(name: String): Boolean = name.lowercase() in supportedMacros

    fun execute(token: TemplateMacroToken.Macro, context: TemplateMacroContext): String? {
        return when (token.name) {
            "setvar" -> {
                val key = token.args.getOrNull(0).orEmpty()
                val value = token.args.drop(1).joinToString("::")
                context.write(key, value)
                ""
            }
            "getvar", "var" -> context.read(token.args.firstOrNull().orEmpty())
            "hasvar" -> booleanString(context.read(token.args.firstOrNull().orEmpty()).isNotBlank())
            "deletevar" -> {
                context.variables.remove(token.args.firstOrNull().orEmpty().trim())
                ""
            }
            "addvar" -> mutateNumber(context.variables, token.args, operation = Double::plus)
            "incvar" -> mutateNumber(context.variables, listOf(token.args.firstOrNull().orEmpty(), "1"), operation = Double::plus)
            "decvar" -> mutateNumber(context.variables, listOf(token.args.firstOrNull().orEmpty(), "1"), operation = Double::minus)
            "setglobalvar" -> {
                val key = token.args.getOrNull(0).orEmpty()
                val value = token.args.drop(1).joinToString("::")
                context.writeGlobal(key, value)
                ""
            }
            "getglobalvar", "globalvar" -> context.readGlobal(token.args.firstOrNull().orEmpty())
            "hasglobalvar" -> booleanString(context.readGlobal(token.args.firstOrNull().orEmpty()).isNotBlank())
            "deleteglobalvar" -> {
                context.globalVariables.remove(token.args.firstOrNull().orEmpty().trim())
                ""
            }
            "addglobalvar" -> mutateNumber(context.globalVariables, token.args, operation = Double::plus)
            "incglobalvar" -> mutateNumber(context.globalVariables, listOf(token.args.firstOrNull().orEmpty(), "1"), operation = Double::plus)
            "decglobalvar" -> mutateNumber(context.globalVariables, listOf(token.args.firstOrNull().orEmpty(), "1"), operation = Double::minus)
            "space" -> " ".repeat(token.args.firstOrNull()?.toIntOrNull()?.coerceAtLeast(1) ?: 1)
            "newline", "br" -> "\n".repeat(token.args.firstOrNull()?.toIntOrNull()?.coerceAtLeast(1) ?: 1)
            "noop" -> ""
            "trim" -> token.args.joinToString("::").trim()
            "default" -> token.args.firstOrNull { it.isNotBlank() }.orEmpty()
            "empty" -> if (token.args.all { it.isBlank() }) "true" else ""
            "reverse" -> token.args.joinToString("::").reversed()
            "lower" -> token.args.joinToString("::").lowercase()
            "upper" -> token.args.joinToString("::").uppercase()
            "capitalize" -> token.args.joinToString("::").replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
            "replace" -> executeReplace(token.args)
            "substring" -> executeSubstring(token.args)
            "if" -> executeIf(token.args)
            "random" -> TemplateMacroRandomSupport.randomChoice(token.args)
            "pick" -> TemplateMacroRandomSupport.stablePick(token.args, context)
            "roll" -> TemplateMacroRandomSupport.roll(token.args.joinToString("::"))
            "hasextension" -> if (token.args.firstOrNull()?.lowercase() in context.extensions.map { it.lowercase() }) "true" else ""
            "charifnotgroup" -> TemplateMacroBuiltinSupport.resolve("charifnotgroup", context)
            "outlet" -> context.readOutlet(token.args.getOrNull(0).orEmpty())
            "banned" -> ""
            // ── SillyTavern-compatible macros ────────────────────────────────
            "idleduration", "idle_duration" -> {
                val lastMsgTime = context.builtin("lastmessagetime").toLongOrNull() ?: 0L
                TemplateMacroTimeSupport.resolveIdleDuration(lastMsgTime)
            }
            "maxcontext", "maxcontexttokens" -> context.builtin("maxcontext").takeIf { it.isNotBlank() }
                ?: context.builtin("maxcontexttokens")
            "maxresponse", "maxresponsetokens" -> context.builtin("maxresponse").takeIf { it.isNotBlank() }
                ?: context.builtin("maxresponsetokens")
            "allchatrange" -> context.builtin("allchatrange")
            "timeutc", "time_utc" -> TemplateMacroTimeSupport.resolveTimeUTC(token.args.firstOrNull().orEmpty())
            else -> null
        } ?: TemplateMacroTimeSupport.resolve(token.name, token.args)
            ?: TemplateMacroBuiltinSupport.resolve(token.name, context)
    }

    fun executeIf(args: List<String>): String {
        val condition = args.getOrNull(0).orEmpty()
        val truthyValue = args.getOrNull(1).orEmpty()
        val falsyValue = args.getOrNull(2).orEmpty()
        return if (isTruthy(condition)) truthyValue else falsyValue
    }

    fun isTruthy(value: String): Boolean {
        val normalized = value.trim().lowercase()
        if (normalized.isBlank()) return false
        return normalized != "false" &&
            normalized != "off" &&
            normalized != "0" &&
            normalized != "null"
    }

    private fun executeReplace(args: List<String>): String {
        val source = args.getOrNull(0).orEmpty()
        val oldValue = args.getOrNull(1).orEmpty()
        val newValue = args.getOrNull(2).orEmpty()
        if (oldValue.isEmpty()) return source
        return source.replace(oldValue, newValue)
    }

    private fun executeSubstring(args: List<String>): String {
        val source = args.getOrNull(0).orEmpty()
        val start = args.getOrNull(1)?.toIntOrNull() ?: 0
        val end = args.getOrNull(2)?.toIntOrNull() ?: source.length
        val safeStart = start.coerceIn(0, source.length)
        val safeEnd = end.coerceIn(safeStart, source.length)
        return source.substring(safeStart, safeEnd)
    }

    private fun mutateNumber(
        target: MutableMap<String, String>,
        args: List<String>,
        operation: (Double, Double) -> Double
    ): String {
        val key = args.getOrNull(0).orEmpty().trim()
        if (key.isBlank()) return ""
        val base = target[key]?.toDoubleOrNull() ?: 0.0
        val delta = args.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val updated = operation(base, delta)
        val normalized = updated.toLong().takeIf { updated == it.toDouble() }?.toString() ?: updated.toString()
        target[key] = normalized
        return normalized
    }

    private fun booleanString(value: Boolean): String = if (value) "true" else ""
}
