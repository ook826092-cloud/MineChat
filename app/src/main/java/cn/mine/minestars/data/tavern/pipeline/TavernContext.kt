package cn.mine.minestars.data.tavern.pipeline

import cn.mine.minestars.core.tavern.preset.PresetEntryData
import cn.mine.minestars.core.tavern.preset.PresetModelParams

/**
 * A single item from the preset's prompt_order array, defining the order
 * and enabled state of prompt entries in the assembled message list.
 */
data class PromptOrderItem(
    val identifier: String,
    val enabled: Boolean,
)

/**
 * Immutable context holding all fully-parsed Tavern data ready for injection
 * into the chat generation pipeline.
 */
data class TavernContext(
    /** Assembled character card prompt from charName/description/personality/scenario, or null */
    val characterCardPrompt: String?,
    /** Parsed preset entries from bound preset, empty if none */
    val presetEntries: List<PresetEntryData>,
    /** Model parameters from the bound preset's rawJson, null if no preset bound */
    val presetModelParams: PresetModelParams? = null,
    /** World book entries that matched conversation context, ready for injection */
    val worldBookInjections: List<WorldBookInjection>,
    /** Loaded regex scripts from bound regex scripts */
    val regexScripts: List<TavernRegexScript>,
    /** Parsed prompt_order from preset rawJson, null for old presets without one */
    val promptOrder: List<PromptOrderItem>? = null,
    /** Whether to merge consecutive system messages before the API call */
    val squashSystemMessages: Boolean = false,
    // ── Utility Prompts ──
    val wiFormat: String = "{0}",
    val impersonationPrompt: String = "",
    val groupNudgePrompt: String = "",
    val newChatPrompt: String = "",
    val newGroupChatPrompt: String = "",
    val newExampleChatPrompt: String = "",
    val continueNudgePrompt: String = "",
    val sendIfEmptyPrompt: String = "",
) {
    val isActive: Boolean get() = characterCardPrompt != null
        || presetEntries.isNotEmpty()
        || worldBookInjections.isNotEmpty()
        || regexScripts.isNotEmpty()
}
