package cn.mine.minestars.core.tavern.worldbook

import android.util.Log

object WorldBookActivator {
    private const val DEFAULT_GROUP_WEIGHT = 100

    private data class TimedEffectState(
        val startIndex: Int,
        val endIndex: Int
    )

    private val timedSticky = linkedMapOf<String, TimedEffectState>()
    private val timedCooldown = linkedMapOf<String, TimedEffectState>()

    fun activate(
        entries: List<WorldBookEntry>,
        context: WorldBookScanContext,
        settings: ChatRequestWorldBookSupport.WorldBookScanSettings,
        trigger: String
    ): List<ActivatedWorldBookEntry> {
        Log.d(WB_TAG, "Start activating world book entries: count=${entries.size}, trigger=$trigger")

        val forced = entries.filter { it.forceActivate && it.enabled && !it.forceDisable }
        val blocked = entries.filter { it.forceDisable }.mapNotNull { it.uid }.toSet()
        val allActivated = mutableListOf<WorldBookEntry>()
        val scanDepth = context.recentMessages.size
        var recursionSteps = 0

        fun scanOnce(depth: Int, state: String) {
            val scanContext = if (state == "recursion" && allActivated.isNotEmpty()) {
                context.copy(recentMessages = context.recentMessages + allActivated.map { it.content })
            } else {
                context
            }
            val eligible = entries.asSequence()
                .filter { it.enabled }
                .filter { entry -> entry.uid == null || entry.uid !in blocked }
                .filter { entry ->
                    if (!matchesCharacterFilter(entry, context)) return@filter false
                    val triggerSet = when (trigger.lowercase()) {
                        "normal" -> setOf("normal", "chat")
                        "continue" -> setOf("continue", "chat")
                        else -> setOf(trigger.lowercase())
                    }
                    if (entry.triggers.isNotEmpty() && !entry.triggers.any { it in triggerSet || it == "always" }) {
                        return@filter false
                    }
                    if (entry.delay != null && depth < entry.delay) return@filter false
                    if (isOnCooldown(entry, context)) return@filter false
                    if (entry.constant) return@filter true
                    if (entry.forceActivate) return@filter true
                    if (state == "recursion" && entry.excludeRecursion) return@filter false
                    if (state != "recursion" && entry.delayUntilRecursion) return@filter false
                    if (entry.preventRecursion && state == "recursion") return@filter false
                    matchesEntry(entry, selectHaystacks(entry, scanContext, depth))
                }
                .filter { entry ->
                    if (isSticky(entry, context)) return@filter true
                    if (!entry.useProbability || entry.probability == null) return@filter true
                    (0..99).random() < entry.probability.coerceIn(0, 100)
                }
                .toList()

            val grouped = mutableMapOf<String, MutableList<WorldBookEntry>>()
            eligible.forEach { entry ->
                val groups = entry.group.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (groups.isEmpty()) {
                    grouped.getOrPut("") { mutableListOf() } += entry
                } else {
                    groups.forEach { group -> grouped.getOrPut(group) { mutableListOf() } += entry }
                }
            }

            grouped.forEach { (group, groupEntries) ->
                if (group.isEmpty()) {
                    allActivated += groupEntries
                    return@forEach
                }

                var candidates = groupEntries.toMutableList()
                val stickyEntries = candidates.filter { isSticky(it, context) }
                if (stickyEntries.isNotEmpty()) candidates = stickyEntries.toMutableList()
                candidates = candidates.filterNot { isOnCooldown(it, context) }.toMutableList()
                val alreadyActivated = allActivated.any { it.group.split(",").map { g -> g.trim() }.contains(group) }
                if (alreadyActivated) return@forEach

                val overrides = candidates.filter { it.groupOverride }
                if (overrides.isNotEmpty()) {
                    allActivated += overrides.first()
                    return@forEach
                }

                val chosen = if (entryUsesGroupWeight(candidates)) {
                    if (candidates.any { it.useGroupScoring == true }) {
                        candidates.maxByOrNull { scoreEntry(it, scanContext) }
                    } else {
                        chooseWeighted(candidates)
                    }
                } else {
                    candidates.maxByOrNull { it.order }
                }
                if (chosen != null) allActivated += chosen
            }
        }

        scanOnce(scanDepth, "initial")

        if (settings.minActivations > 0 && allActivated.size < settings.minActivations) {
            var skew = 1
            while (allActivated.size < settings.minActivations) {
                val nextDepth = scanDepth + skew
                if (settings.minActivationsDepthMax > 0 && nextDepth > settings.minActivationsDepthMax) break
                scanOnce(nextDepth, "min_activations")
                skew += 1
            }
        }

        while (settings.maxRecursionSteps > 0 && recursionSteps < settings.maxRecursionSteps) {
            recursionSteps += 1
            scanOnce(scanDepth, "recursion")
        }

        val combined = (allActivated + forced)
            .distinctBy { it.uid ?: it.title + it.content }

        updateTimedEffects(combined, context)

        return combined.map {
            ActivatedWorldBookEntry(
                uid = it.uid,
                title = it.title,
                content = it.content,
                position = it.position,
                depth = it.depth,
                order = it.order,
                role = it.role,
                outletName = it.outletName
            )
        }
    }

    private fun chooseWeighted(candidates: List<WorldBookEntry>): WorldBookEntry? {
        val totalWeight = candidates.sumOf { it.groupWeight ?: DEFAULT_GROUP_WEIGHT }
        if (totalWeight <= 0) return candidates.firstOrNull()
        var roll = (0..totalWeight).random()
        return candidates.firstOrNull {
            roll -= (it.groupWeight ?: DEFAULT_GROUP_WEIGHT)
            roll <= 0
        } ?: candidates.firstOrNull()
    }

    private fun scoreEntry(entry: WorldBookEntry, context: WorldBookScanContext): Int {
        val haystacks = selectHaystacks(entry, context)
        var score = 0
        entry.keys.forEach { key ->
            if (haystacks.any { containsNeedle(it, key, entry.caseSensitive ?: false, entry.matchWholeWords ?: false, entry.useRegex) }) {
                score += 2
            }
        }
        entry.secondaryKeys.forEach { key ->
            if (haystacks.any { containsNeedle(it, key, entry.caseSensitive ?: false, entry.matchWholeWords ?: false, entry.useRegex) }) {
                score += 1
            }
        }
        return score
    }

    private fun isOnCooldown(entry: WorldBookEntry, context: WorldBookScanContext): Boolean {
        val key = entry.uid?.toString() ?: entry.title + entry.content
        val now = context.recentMessages.size
        return timedCooldown[key]?.let { now <= it.endIndex } == true
    }

    private fun isSticky(entry: WorldBookEntry, context: WorldBookScanContext): Boolean {
        val key = entry.uid?.toString() ?: entry.title + entry.content
        val now = context.recentMessages.size
        return timedSticky[key]?.let { now <= it.endIndex } == true
    }

    private fun updateTimedEffects(entries: List<WorldBookEntry>, context: WorldBookScanContext) {
        val now = context.recentMessages.size
        entries.forEach { entry ->
            val key = entry.uid?.toString() ?: entry.title + entry.content
            if (entry.sticky != null && entry.sticky > 0) {
                timedSticky[key] = TimedEffectState(now, now + entry.sticky)
            }
            if (entry.cooldown != null && entry.cooldown > 0) {
                timedCooldown[key] = TimedEffectState(now, now + entry.cooldown)
            }
        }
    }

    private fun selectHaystacks(entry: WorldBookEntry, context: WorldBookScanContext, depth: Int? = null): List<String> {
        val selected = mutableListOf<String>()
        val historyText = context.recentMessages
            .let { messages ->
                val effectiveDepth = entry.scanDepth ?: depth
                if (effectiveDepth == null || effectiveDepth <= 0) messages else messages.takeLast(effectiveDepth)
            }
            .joinToString("\n")
            .trim()
        if (historyText.isNotBlank()) selected += historyText
        if (context.currentMessage.isNotBlank()) selected += context.currentMessage
        if (entry.matchPersonaDescription) selected += context.personaDescription
        if (entry.matchCharacterDescription) selected += context.characterDescription
        if (entry.matchCharacterPersonality) selected += context.characterPersonality
        if (entry.matchCharacterDepthPrompt) selected += context.characterDepthPrompt
        if (entry.matchScenario) selected += context.scenario
        if (entry.matchCreatorNotes) selected += context.creatorNotes
        return selected.filter { it.isNotBlank() }
    }

    private fun matchesEntry(entry: WorldBookEntry, haystacks: List<String>): Boolean {
        if (haystacks.isEmpty()) return false
        val primaryMatched = if (entry.keys.isEmpty()) true else entry.keys.any { key ->
            haystacks.any { haystack ->
                containsNeedle(
                    haystack = haystack,
                    needle = key,
                    caseSensitive = entry.caseSensitive ?: false,
                    wholeWord = entry.matchWholeWords ?: false,
                    useRegex = entry.useRegex
                )
            }
        }
        if (!primaryMatched) return false
        if (!entry.selective || entry.secondaryKeys.isEmpty()) return true

        val logic = entry.selectiveLogic ?: 0
        var hasAnyMatch = false
        var hasAllMatch = true
        entry.secondaryKeys.forEach { key ->
            val matched = haystacks.any { haystack ->
                containsNeedle(
                    haystack = haystack,
                    needle = key,
                    caseSensitive = entry.caseSensitive ?: false,
                    wholeWord = entry.matchWholeWords ?: false,
                    useRegex = entry.useRegex
                )
            }
            if (matched) hasAnyMatch = true else hasAllMatch = false
            if (logic == 0 && matched) return true
            if (logic == 1 && !matched) return true
        }

        if (logic == 2 && !hasAnyMatch) return true
        if (logic == 3 && hasAllMatch) return true
        return false
    }

    private fun containsNeedle(
        haystack: String,
        needle: String,
        caseSensitive: Boolean,
        wholeWord: Boolean,
        useRegex: Boolean
    ): Boolean {
        if (needle.isBlank()) return false
        if (useRegex) {
            val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val regex = runCatching { Regex(needle, options) }.getOrNull() ?: return false
            return regex.containsMatchIn(haystack)
        }
        if (!wholeWord) return haystack.contains(needle, ignoreCase = !caseSensitive)
        val escaped = Regex.escape(needle)
        val pattern = Regex("\\b$escaped\\b", if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
        return pattern.containsMatchIn(haystack)
    }

    private fun entryUsesGroupWeight(entries: List<WorldBookEntry>): Boolean {
        return entries.any { it.useGroupScoring == true }
    }

    private fun matchesCharacterFilter(entry: WorldBookEntry, context: WorldBookScanContext): Boolean {
        if (entry.characterFilterNames.isEmpty() && entry.characterFilterTags.isEmpty()) return true
        val name = context.characterName.trim()
        val tags = context.characterTags.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val nameMatched = entry.characterFilterNames.any { it.equals(name, ignoreCase = true) }
        val tagMatched = entry.characterFilterTags.any { it in tags }
        val matched = nameMatched || tagMatched
        return if (entry.characterFilterExclude) !matched else matched
    }
}
