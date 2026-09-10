package cn.mine.minestars.core.tavern.macro

object TemplateMacroVariableStore {
    private val assistantScopedVariables = linkedMapOf<String, MutableMap<String, String>>()
    private val globalVariables = linkedMapOf<String, String>()

    fun assistantScope(scopeKey: String, seed: Map<String, String> = emptyMap()): MutableMap<String, String> {
        val target = assistantScopedVariables.getOrPut(scopeKey) { linkedMapOf() }
        if (seed.isNotEmpty()) {
            target.putAll(seed)
        }
        return target
    }

    fun globalScope(seed: Map<String, String> = emptyMap()): MutableMap<String, String> {
        if (seed.isNotEmpty()) {
            globalVariables.putAll(seed)
        }
        return globalVariables
    }
}
