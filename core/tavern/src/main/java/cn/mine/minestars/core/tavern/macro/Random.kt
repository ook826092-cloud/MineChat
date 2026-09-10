package cn.mine.minestars.core.tavern.macro

import kotlin.math.absoluteValue
import kotlin.random.Random

object TemplateMacroRandomSupport {
    fun randomChoice(args: List<String>): String {
        val options = normalizeList(args)
        if (options.isEmpty()) return ""
        return options.random()
    }

    fun stablePick(args: List<String>, context: TemplateMacroContext): String {
        val options = normalizeList(args)
        if (options.isEmpty()) return ""
        // Deterministic pick based solely on option values, not variables state.
        // This ensures the same {{pick::A::B::C}} always picks the same item.
        val seed = options.joinToString("|").hashCode().absoluteValue
        val index = seed % options.size
        return options[index]
    }

    fun roll(rawFormula: String): String {
        var formula = rawFormula.trim()
        if (formula.isBlank()) return ""
        if (formula.all { it.isDigit() }) {
            formula = "1d$formula"
        }
        val parsed = Regex("^(\\d+)[dD](\\d+)([+-]\\d+)?$").matchEntire(formula) ?: return ""
        val count = parsed.groupValues[1].toIntOrNull()?.coerceIn(1, 100) ?: return ""
        val sides = parsed.groupValues[2].toIntOrNull()?.coerceIn(1, 1000) ?: return ""
        val modifier = parsed.groupValues[3].toIntOrNull() ?: 0
        val total = (1..count).sumOf { Random.nextInt(1, sides + 1) } + modifier
        return total.toString()
    }

    private fun normalizeList(args: List<String>): List<String> {
        val flattened = if (args.size == 1) {
            args.first()
                .split("::", ",")
                .map { it.trim() }
        } else {
            args.map { it.trim() }
        }
        return flattened.filter { it.isNotBlank() }
    }
}
