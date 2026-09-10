package cn.mine.minestars.core.tavern.worldbook

import cn.mine.minestars.core.tavern.regex.ChatRequestRegexSupport
import cn.mine.minestars.core.tavern.regex.RegexDestination
import cn.mine.minestars.core.tavern.regex.RegexScript
import cn.mine.minestars.core.tavern.regex.RegexSource

object WorldBookMessageSupport {
    fun buildMessage(title: String?, content: String): String {
        return if (title.isNullOrBlank()) {
            "World info:\n$content"
        } else {
            "World info [$title]:\n$content"
        }
    }

    fun injectDepthEntries(
        historyDialogue: List<Pair<String, String>>,
        entries: List<DepthInjection>
    ): List<Pair<String, String>> {
        if (entries.isEmpty()) return historyDialogue
        val grouped = entries.groupBy { it.depth }
        val result = mutableListOf<Pair<String, String>>()

        grouped.filterKeys { it >= historyDialogue.size }
            .toSortedMap(compareByDescending { it })
            .values
            .flatten()
            .sortedByDescending { it.order }
            .forEach { result += it.role to it.content }

        historyDialogue.forEachIndexed { index, pair ->
            val depthFromLatest = historyDialogue.lastIndex - index
            grouped[depthFromLatest]
                .orEmpty()
                .sortedByDescending { it.order }
                .forEach { result += it.role to it.content }
            result += pair
        }

        grouped.filterKeys { it < 0 }
            .toSortedMap()
            .values
            .flatten()
            .sortedByDescending { it.order }
            .forEach { result += it.role to it.content }

        return result
    }

    fun appendEntries(
        target: MutableList<Pair<String, String>>,
        entries: List<ActivatedWorldBookEntry>,
        applyMacros: (String) -> String,
        regexScripts: List<RegexScript>
    ) {
        val beforeEntries = entries.filter { entry ->
            val pos = entry.position
            pos != 1 && pos != 4 && pos != 5 && pos != 6 && pos != 7
        }
        val afterEntries = entries.filter { it.position == 1 }
        val depthEntries = entries.filter { it.position == 4 }
        val outletEntries = entries.filter { it.position == 7 }
        val exampleBeforeEntries = entries.filter { it.position == 5 }
        val exampleAfterEntries = entries.filter { it.position == 6 }

        beforeEntries.sortedBy { it.order }.forEach { entry ->
            appendEntry(target, entry, applyMacros, regexScripts, Int.MAX_VALUE)
        }
        depthEntries.sortedBy { it.order }.forEach { entry ->
            appendEntry(target, entry, applyMacros, regexScripts, entry.depth ?: Int.MAX_VALUE)
        }
        afterEntries.sortedBy { it.order }.forEach { entry ->
            appendEntry(target, entry, applyMacros, regexScripts, Int.MAX_VALUE)
        }
        exampleBeforeEntries.forEach { entry ->
            appendEntry(target, entry, applyMacros, regexScripts, Int.MAX_VALUE)
        }
        exampleAfterEntries.forEach { entry ->
            appendEntry(target, entry, applyMacros, regexScripts, Int.MAX_VALUE)
        }
        outletEntries.forEach { entry ->
            val content = renderEntryContent(entry, applyMacros, regexScripts, Int.MAX_VALUE)
            if (content.isNotBlank()) {
                val outletName = entry.outletName ?: entry.title ?: "default"
                target += "system" to "[OUTLET::$outletName]$content"
            }
        }
    }

    private fun appendEntry(
        target: MutableList<Pair<String, String>>,
        entry: ActivatedWorldBookEntry,
        applyMacros: (String) -> String,
        regexScripts: List<RegexScript>,
        depthFromLatest: Int
    ) {
        val content = renderEntryContent(entry, applyMacros, regexScripts, depthFromLatest)
        if (content.isNotBlank()) {
            target += entry.role to buildMessage(entry.title, content)
        }
    }

    private fun renderEntryContent(
        entry: ActivatedWorldBookEntry,
        applyMacros: (String) -> String,
        regexScripts: List<RegexScript>,
        depthFromLatest: Int
    ): String {
        return ChatRequestRegexSupport.applyRegexScripts(
            text = applyMacros(entry.content).trim(),
            scripts = regexScripts,
            depthFromLatest = depthFromLatest,
            source = RegexSource.WORLD_INFO,
            destination = RegexDestination.PROMPT
        ).trim()
    }
}
