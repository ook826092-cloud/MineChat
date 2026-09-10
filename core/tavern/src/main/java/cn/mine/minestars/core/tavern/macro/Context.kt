package cn.mine.minestars.core.tavern.macro

data class TemplateMacroContext(
    val variables: MutableMap<String, String> = linkedMapOf(),
    val globalVariables: MutableMap<String, String> = linkedMapOf(),
    val builtins: Map<String, String> = emptyMap(),
    val outlets: MutableMap<String, String> = linkedMapOf(),
    /** Set of available extension tool names (for {{hasExtension::name}} checking) */
    val extensions: Set<String> = emptySet(),
) {
    fun read(key: String): String = variables[key.trim()].orEmpty()

    fun write(key: String, value: String) {
        val normalizedKey = key.trim()
        if (normalizedKey.isNotBlank()) {
            variables[normalizedKey] = value
        }
    }

    fun readGlobal(key: String): String = globalVariables[key.trim()].orEmpty()

    fun writeGlobal(key: String, value: String) {
        val normalizedKey = key.trim()
        if (normalizedKey.isNotBlank()) {
            globalVariables[normalizedKey] = value
        }
    }

    fun builtin(key: String): String = builtins[key.trim().lowercase()].orEmpty()

    fun readOutlet(key: String): String = outlets[key.trim()].orEmpty()

    fun writeOutlet(key: String, value: String) {
        val normalizedKey = key.trim()
        if (normalizedKey.isNotBlank()) {
            outlets[normalizedKey] = value
        }
    }
}
