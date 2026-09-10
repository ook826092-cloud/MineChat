package cn.mine.minestars.data.ai.transformers

import cn.mine.ai.ui.UIMessage

/**
 * USER_INPUT regex is now applied early in TavernDataLoader.loadContext()
 * (before WI scan), matching SillyTavern's order of operations.
 * This transformer is a no-op placeholder for backward compatibility.
 */
@Deprecated("Replaced by early regex pass in TavernDataLoader.applyRegexToMessages()")
object TavernRegexInputTransformer : InputMessageTransformer {
    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> = messages
}
