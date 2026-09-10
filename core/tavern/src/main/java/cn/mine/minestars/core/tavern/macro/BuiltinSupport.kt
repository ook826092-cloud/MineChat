package cn.mine.minestars.core.tavern.macro

object TemplateMacroBuiltinSupport {
    private val builtinNames = setOf(
        "user",
        "char",
        "character",
        "assistant",
        "name1",
        "name2",
        "charifnotgroup",
        "group",
        "groupnotmuted",
        "notchar",
        "charprompt",
        "charinstruction",
        "chardescription",
        "charpersonality",
        "charscenario",
        "persona",
        "personadescription",
        "personadescriptionraw",
        "user_description",
        "mesexamplesraw",
        "mesexamples",
        "chardepthprompt",
        "charcreatornotes",
        "charversion",
        "systemprompt",
        "system",
        "main",
        "model",
        "input",
        "original",
        "ismobile",
        "maxprompt",
        "lastmessage",
        "lastmessageid",
        "lastusermessage",
        "lastcharmessage",
        "lastusermessageid",
        "lastcharmessageid",
        "firstincludedmessageid",
        "firstdisplayedmessageid",
        "lastswipeid",
        "currentswipeid",
        "lastgenerationtype",
        // SillyTavern-compatible macros
        "idleduration",
        "idle_duration",
        "maxcontext",
        "maxcontexttokens",
        "maxresponse",
        "maxresponsetokens",
        "allchatrange",
        "timeutc",
        "time_utc",
        "isotime",
        "isodate",
        "datetimeformat",
        "timediff"
    )

    fun isBuiltin(name: String): Boolean = name.lowercase() in builtinNames

    fun resolve(name: String, context: TemplateMacroContext): String? {
        return context.builtin(name)
            .takeIf { it.isNotEmpty() || isBuiltin(name) }
    }
}
