package cn.mine.minestars.data.ai.transformers

import cn.mine.ai.core.MessageRole
import cn.mine.ai.ui.UIMessage
import cn.mine.ai.ui.UIMessagePart
import cn.mine.minestars.data.tavern.pipeline.TavernRegexPlacement
import cn.mine.minestars.data.tavern.pipeline.TavernRegexRunner
import cn.mine.minestars.data.tavern.pipeline.TavernRegexScript

/**
 * Applies SillyTavern-compatible regex to AI output messages.
 *
 * - transform(): Applies default scripts (non-markdownOnly, non-promptOnly)
 *   to AI output text before storage, matching ST's cleanUpMessage behavior.
 * - visualTransform(): Applies markdownOnly scripts during display rendering.
 *
 * promptOnly scripts are handled by TavernDataLoader's early regex pass.
 */
object TavernRegexOutputTransformer : OutputMessageTransformer {
    private const val TAG = "TavernRegex"

    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val scripts = ctx.tavernRegexScripts
        if (scripts.isEmpty()) return messages
        return applyRegex(messages, scripts, ctx, isMarkdown = false)
    }

    override suspend fun visualTransform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val scripts = ctx.tavernRegexScripts
        if (scripts.isEmpty()) return messages
        return applyRegex(messages, scripts, ctx, isMarkdown = true)
    }

    private fun applyRegex(
        messages: List<UIMessage>,
        scripts: List<TavernRegexScript>,
        ctx: TransformerContext,
        isMarkdown: Boolean,
    ): List<UIMessage> {
        return messages.map { message ->
            if (message.role != MessageRole.ASSISTANT) return@map message

            message.copy(
                parts = message.parts.map { part ->
                    when (part) {
                        is UIMessagePart.Text -> {
                            val result = TavernRegexRunner.apply(
                                text = part.text,
                                scripts = scripts,
                                placement = TavernRegexPlacement.AI_OUTPUT,
                                macroContext = ctx.tavernMacroContext,
                                isPrompt = false,
                                isMarkdown = isMarkdown,
                            )
                            part.copy(text = result)
                        }
                        is UIMessagePart.Reasoning -> {
                            val result = TavernRegexRunner.apply(
                                text = part.reasoning,
                                scripts = scripts,
                                placement = TavernRegexPlacement.REASONING,
                                macroContext = ctx.tavernMacroContext,
                                isPrompt = false,
                                isMarkdown = isMarkdown,
                            )
                            part.copy(reasoning = result)
                        }
                        else -> part
                    }
                }
            )
        }
    }
}
