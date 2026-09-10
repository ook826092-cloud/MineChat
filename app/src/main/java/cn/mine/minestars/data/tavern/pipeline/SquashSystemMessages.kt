package cn.mine.minestars.data.tavern.pipeline

import cn.mine.ai.core.MessageRole
import cn.mine.ai.ui.UIMessage

/**
 * Merges consecutive SYSTEM-role messages by joining their text with newlines.
 * Non-SYSTEM messages act as boundaries that prevent merging across them.
 *
 * Mirrors SillyTavern's [ChatCompletion.squashSystemMessages](),
 * which combines consecutive `role: 'system'` messages (without names)
 * into a single message before the API call.
 *
 * Usage:
 * ```kotlin
 * val squashed = messages.squashSystemMessages()
 * ```
 */
fun List<UIMessage>.squashSystemMessages(): List<UIMessage> {
    val result = mutableListOf<UIMessage>()
    val pendingTexts = mutableListOf<String>()

    for (msg in this) {
        if (msg.role == MessageRole.SYSTEM) {
            pendingTexts.add(msg.toText())
        } else {
            if (pendingTexts.isNotEmpty()) {
                result.add(UIMessage.system(pendingTexts.joinToString("\n")))
                pendingTexts.clear()
            }
            result.add(msg)
        }
    }
    if (pendingTexts.isNotEmpty()) {
        result.add(UIMessage.system(pendingTexts.joinToString("\n")))
    }
    return result
}
