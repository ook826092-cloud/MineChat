package cn.mine.minestars.data.tavern.pipeline

/**
 * Parsed SillyTavern regex script — JSON keys exactly match ST format.
 */
data class TavernRegexScript(
    val scriptName: String,
    val findRegex: String,
    val replaceString: String,
    val trimStrings: List<String> = emptyList(),
    val disabled: Boolean = false,
    val promptOnly: Boolean = false,
    val markdownOnly: Boolean = false,
    val substituteRegex: Int = 0,
    val placement: List<Int> = emptyList(),
    val runOnEdit: Boolean = false,
    val minDepth: Int? = null,
    val maxDepth: Int? = null,
)

/**
 * A single world book entry that matched and is ready for injection.
 */
data class WorldBookInjection(
    val content: String,
    val role: String = "system",
    val depth: Int = 4,
    val order: Int = 100,
    val position: InjectionPosition = InjectionPosition.AT_DEPTH,
)

/**
 * Where in the prompt context this entry should be inserted.
 *   BEFORE_PROMPT — as a separate system message before the main system prompt  (ST position 0)
 *   AFTER_PROMPT  — as a separate system message after the main system prompt   (ST position 1)
 *   AT_DEPTH      — injected at a specific depth into the chat history           (ST position 4)
 */
enum class InjectionPosition { BEFORE_PROMPT, AFTER_PROMPT, AT_DEPTH }
