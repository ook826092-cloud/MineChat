package cn.mine.minestars.core.tavern.worldbook

import cn.mine.minestars.core.tavern.regex.RegexScript

const val WB_TAG = "TavernWorldBook"

data class WorldBookScanContext(
    val recentMessages: List<String>,
    val currentMessage: String,
    val personaDescription: String,
    val characterDescription: String,
    val characterPersonality: String,
    val characterDepthPrompt: String,
    val scenario: String,
    val creatorNotes: String,
    val characterName: String,
    val characterTags: List<String>
) {
    val combinedHistory: String get() = recentMessages.joinToString("\n").trim()
    val combinedChat: String
        get() = listOf(combinedHistory, currentMessage)
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()
    val combinedGlobal: String
        get() = listOf(
            combinedChat,
            personaDescription,
            characterDescription,
            characterPersonality,
            characterDepthPrompt,
            scenario,
            creatorNotes
        ).filter { it.isNotBlank() }.joinToString("\n")
}

data class WorldBookEntry(
    val uid: Int?,
    val title: String?,
    val content: String,
    val decorators: List<String>,
    val forceActivate: Boolean,
    val forceDisable: Boolean,
    val keys: List<String>,
    val secondaryKeys: List<String>,
    val enabled: Boolean,
    val constant: Boolean,
    val vectorized: Boolean,
    val position: Int?,
    val depth: Int?,
    val order: Int,
    val selective: Boolean,
    val selectiveLogic: Int?,
    val useRegex: Boolean,
    val scanDepth: Int?,
    val probability: Int?,
    val useProbability: Boolean,
    val group: String,
    val groupOverride: Boolean,
    val groupWeight: Int?,
    val sticky: Int?,
    val cooldown: Int?,
    val delay: Int?,
    val caseSensitive: Boolean?,
    val matchWholeWords: Boolean?,
    val matchPersonaDescription: Boolean,
    val matchCharacterDescription: Boolean,
    val matchCharacterPersonality: Boolean,
    val matchCharacterDepthPrompt: Boolean,
    val matchScenario: Boolean,
    val matchCreatorNotes: Boolean,
    val delayUntilRecursion: Boolean,
    val preventRecursion: Boolean,
    val excludeRecursion: Boolean,
    val useGroupScoring: Boolean?,
    val automationId: String?,
    val role: String,
    val outletName: String?,
    val triggers: List<String>,
    val ignoreBudget: Boolean,
    val characterFilterNames: List<String>,
    val characterFilterTags: List<String>,
    val characterFilterExclude: Boolean
)

data class ActivatedWorldBookEntry(
    val uid: Int?,
    val title: String?,
    val content: String,
    val position: Int?,
    val depth: Int?,
    val order: Int,
    val role: String,
    val outletName: String?
)

data class DepthInjection(
    val depth: Int,
    val role: String,
    val content: String,
    val order: Int
)

object ChatRequestWorldBookSupport {
    private const val DEFAULT_TRIGGER = "normal"
    private const val DEFAULT_MAX_RECURSION_STEPS = 3
    private const val DEFAULT_MIN_ACTIVATIONS = 0
    private const val DEFAULT_MIN_ACTIVATIONS_DEPTH_MAX = 0
    private const val DEFAULT_INSERTION_STRATEGY = 1

    data class WorldBookScanSettings(
        val minActivations: Int = DEFAULT_MIN_ACTIVATIONS,
        val minActivationsDepthMax: Int = DEFAULT_MIN_ACTIVATIONS_DEPTH_MAX,
        val maxRecursionSteps: Int = DEFAULT_MAX_RECURSION_STEPS,
        val insertionStrategy: Int = DEFAULT_INSERTION_STRATEGY
    )

    fun buildWorldBookMessage(title: String?, content: String): String {
        return WorldBookMessageSupport.buildMessage(title, content)
    }

    fun injectDepthEntries(
        historyDialogue: List<Pair<String, String>>,
        entries: List<DepthInjection>
    ): List<Pair<String, String>> {
        return WorldBookMessageSupport.injectDepthEntries(historyDialogue, entries)
    }

    fun parseWorldBookEntries(rawJson: String): List<WorldBookEntry> {
        return WorldBookParser.parse(rawJson)
    }

    fun activateWorldBookEntries(
        entries: List<WorldBookEntry>,
        context: WorldBookScanContext,
        settings: WorldBookScanSettings = WorldBookScanSettings(),
        trigger: String = DEFAULT_TRIGGER
    ): List<ActivatedWorldBookEntry> {
        return WorldBookActivator.activate(entries, context, settings, trigger)
    }

    fun appendWorldBookEntries(
        target: MutableList<Pair<String, String>>,
        entries: List<ActivatedWorldBookEntry>,
        applyMacros: (String) -> String,
        regexScripts: List<RegexScript>
    ) {
        WorldBookMessageSupport.appendEntries(target, entries, applyMacros, regexScripts)
    }
}
