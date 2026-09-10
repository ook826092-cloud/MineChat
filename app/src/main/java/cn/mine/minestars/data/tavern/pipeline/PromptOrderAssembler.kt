package cn.mine.minestars.data.tavern.pipeline

import android.util.Log
import cn.mine.ai.core.MessageRole
import cn.mine.ai.ui.UIMessage
import cn.mine.minestars.core.tavern.macro.TavernMacroContext
import cn.mine.minestars.core.tavern.macro.TavernMacroResolver
import cn.mine.minestars.core.tavern.macro.TemplateMacroContext
import cn.mine.minestars.core.tavern.macro.TemplateMacroEngine
import cn.mine.minestars.core.tavern.preset.PresetEntryData
import cn.mine.minestars.data.model.Assistant

private const val TAG = "PromptOrderAssembler"

/**
 * Assembles a list of [UIMessage] by iterating the preset's [PromptOrderItem] list
 * in sequence, resolving each identifier against preset entries, character card data,
 * world book injections, and conversation history.
 *
 * This replaces the old "three-phase" hardcoded assembly with SillyTavern-compatible
 * prompt_order-driven ordering.
 */
class PromptOrderAssembler {

    /**
     * Temporary reference to [TemplateMacroContext] set per [assemble] call.
     * Used by [resolveWithTemplates] for the TemplateMacroEngine second pass.
     */
    private var _templateCtx: TemplateMacroContext? = null

    /**
     * Build the final message list from prompt_order items.
     *
     * @param supplementMessages Pre-built system messages for memories, tools, etc.
     *   These are inserted at the chatHistory boundary (or appended if no chatHistory).
     */
    fun assemble(
        promptOrder: List<PromptOrderItem>,
        presetEntries: List<PresetEntryData>,
        characterCardPrompt: String?,
        assistant: Assistant,
        userPersona: String,
        worldBookBefore: List<WorldBookInjection>,
        worldBookAfter: List<WorldBookInjection>,
        worldBookDepth: List<WorldBookInjection>,
        historyMessages: List<UIMessage>,
        macroContext: TavernMacroContext,
        regexScripts: List<TavernRegexScript>,
        supplementMessages: List<UIMessage> = emptyList(),
        templateMacroContext: TemplateMacroContext? = null,
    ): List<UIMessage> {
        _templateCtx = templateMacroContext
        val entryByIdentifier = presetEntries.associateBy { it.identifier.lowercase() }
        val mountedIdentifiers = promptOrder.map { it.identifier.lowercase() }.toSet()

        // Unmounted entries with injectionPosition=1 → append after prompt_order
        val unmountedPosition1 = presetEntries.filter { entry ->
            entry.identifier.lowercase() !in mountedIdentifiers
                && entry.enabled
                && entry.injectionPosition == 1
                && entry.content.isNotBlank()
        }.sortedBy { it.injectionOrder ?: 0 }

        // Unmounted entries with injectionPosition=4 → depth injection into chatHistory
        val unmountedDepth = presetEntries.filter { entry ->
            entry.identifier.lowercase() !in mountedIdentifiers
                && entry.enabled
                && entry.injectionPosition == 4
                && entry.injectionDepth != null
                && entry.content.isNotBlank()
        }

        val result = mutableListOf<UIMessage>()
        var chatHistoryInsertIndex = -1

        // Phase 1: Process prompt_order items in sequence
        for (orderItem in promptOrder) {
            if (!orderItem.enabled) continue

            val identifier = orderItem.identifier.lowercase()
            val entry = entryByIdentifier[identifier]

            // Skip if the entry itself is disabled
            if (entry != null && !entry.enabled) continue

            when (identifier) {
                "chathistory" -> {
                    chatHistoryInsertIndex = result.size

                    // Insert supplement messages (memories, tools) right before chat history
                    if (supplementMessages.isNotEmpty()) {
                        result.addAll(supplementMessages)
                    }

                    // Base history with context limit
                    val baseHistory = historyMessages.toMutableList()

                    // Collect depth injections: world book AT_DEPTH + unmounted preset AT_DEPTH
                    val allDepthInjections = buildList {
                        // World book depth entries
                        addAll(worldBookDepth)

                        // Unmounted preset entries with injectionPosition=4
                        unmountedDepth.forEach { entry ->
                            add(
                                WorldBookInjection(
                                    content = entry.content,
                                    role = entry.role.ifBlank { "system" },
                                    depth = entry.injectionDepth ?: 4,
                                    order = entry.injectionOrder ?: 0,
                                    position = InjectionPosition.AT_DEPTH,
                                )
                            )
                        }
                    }

                    // Sort: deepest first, then by order ascending
                    val sortedDepth = allDepthInjections.sortedWith(
                        compareByDescending<WorldBookInjection> { it.depth }
                            .thenBy { it.order }
                    )

                    // Apply depth injections
                    if (sortedDepth.isNotEmpty()) {
                        val injected = baseHistory.toMutableList()
                        for (injection in sortedDepth) {
                            val text = resolveWorldInfoText(
                                content = injection.content,
                                depth = injection.depth,
                                regexScripts = regexScripts,
                                macroContext = macroContext,
                            )
                            if (text == null) continue
                            val role = when (injection.role.lowercase()) {
                                "user" -> MessageRole.USER
                                "assistant" -> MessageRole.ASSISTANT
                                else -> MessageRole.SYSTEM
                            }
                            val msg = when (role) {
                                MessageRole.USER -> UIMessage.user(text)
                                MessageRole.ASSISTANT -> UIMessage.assistant(text)
                                else -> UIMessage.system(prompt = text)
                            }
                            val insertAt = (injected.size - injection.depth).coerceIn(0, injected.size)
                            injected.add(insertAt, msg)
                        }
                        result.addAll(injected)
                    } else {
                        result.addAll(baseHistory)
                    }
                }

                "worldinfobefore" -> {
                    worldBookBefore.sortedBy { it.order }.forEach { wb ->
                        val msg = resolveWorldInfoContent(
                            content = wb.content,
                            role = wb.role,
                            depth = wb.depth,
                            regexScripts = regexScripts,
                            macroContext = macroContext,
                        )
                        if (msg != null) result.add(msg)
                    }
                }

                "worldinfoafter" -> {
                    worldBookAfter.sortedBy { it.order }.forEach { wb ->
                        val msg = resolveWorldInfoContent(
                            content = wb.content,
                            role = wb.role,
                            depth = wb.depth,
                            regexScripts = regexScripts,
                            macroContext = macroContext,
                        )
                        if (msg != null) result.add(msg)
                    }
                }

                "dialogueexamples" -> {
                    resolveDialogueExamples(entry, macroContext)?.let { result.add(it) }
                }

                else -> {
                    // All other entries: main, jailbreak, charDescription, custom, etc.
                    val content = resolveEntryContent(
                        identifier = identifier,
                        entry = entry,
                        characterCardPrompt = characterCardPrompt,
                        assistant = assistant,
                        userPersona = userPersona,
                        macroContext = macroContext,
                    )
                    if (content.isBlank()) continue

                    val role = when (entry?.role?.lowercase()) {
                        "user" -> MessageRole.USER
                        "assistant" -> MessageRole.ASSISTANT
                        else -> MessageRole.SYSTEM
                    }
                    val msg = when (role) {
                        MessageRole.USER -> UIMessage.user(content)
                        MessageRole.ASSISTANT -> UIMessage.assistant(content)
                        else -> UIMessage.system(prompt = content)
                    }
                    result.add(msg)
                }
            }
        }

        // Phase 2: Append unmounted position=1 entries after prompt_order items
        unmountedPosition1.forEach { entry ->
            val content = resolveWithTemplates(entry.content, macroContext)
            if (content.isBlank()) return@forEach
            val role = when (entry.role.lowercase()) {
                "user" -> MessageRole.USER
                "assistant" -> MessageRole.ASSISTANT
                else -> MessageRole.SYSTEM
            }
            val msg = when (role) {
                MessageRole.USER -> UIMessage.user(content)
                MessageRole.ASSISTANT -> UIMessage.assistant(content)
                else -> UIMessage.system(prompt = content)
            }
            result.add(msg)
        }

        // Phase 3: Fallback world book positioning if not in prompt_order
        val hasWbBefore = promptOrder.any { it.identifier.equals("worldinfobefore", ignoreCase = true) }
        val hasWbAfter = promptOrder.any { it.identifier.equals("worldinfoafter", ignoreCase = true) }

        if (!hasWbBefore && worldBookBefore.isNotEmpty()) {
            Log.d(TAG, "worldInfoBefore not in prompt_order, prepending ${worldBookBefore.size} entries")
            val beforeMessages = worldBookBefore.sortedBy { it.order }.mapNotNull { wb ->
                resolveWorldInfoContent(wb.content, wb.role, wb.depth, regexScripts, macroContext)
            }
            result.addAll(0, beforeMessages)
        }

        if (!hasWbAfter && worldBookAfter.isNotEmpty()) {
            Log.d(TAG, "worldInfoAfter not in prompt_order, inserting ${worldBookAfter.size} entries")
            val afterMessages = worldBookAfter.sortedBy { it.order }.mapNotNull { wb ->
                resolveWorldInfoContent(wb.content, wb.role, wb.depth, regexScripts, macroContext)
            }
            val insertAt = if (chatHistoryInsertIndex >= 0) chatHistoryInsertIndex else result.size
            result.addAll(insertAt, afterMessages)
        }

        // Phase 4: If no chatHistory in prompt_order, append supplement messages at end
        if (chatHistoryInsertIndex < 0 && supplementMessages.isNotEmpty()) {
            result.addAll(supplementMessages)
        }

        Log.d(TAG, "Assembled ${result.size} messages from ${promptOrder.size} prompt_order items")
        _templateCtx = null
        return result
    }

    /**
     * Two-pass macro resolution: first [TavernMacroResolver] for legacy tavern macros
     * ({{user}}, {{char}}, {{original}}, {{greeting::N}}, etc.),
     * then [TemplateMacroEngine] for setvar/getvar and all extended macros.
     *
     * When [templateCtx] is null, falls back to [TavernMacroResolver] only.
     */
    private fun resolveWithTemplates(
        text: String,
        tavernCtx: TavernMacroContext,
    ): String {
        val firstPass = TavernMacroResolver.resolve(text, tavernCtx)
        val ctx = _templateCtx ?: return firstPass
        return TemplateMacroEngine.resolve(firstPass, ctx)
    }

    // ─── Entry content resolution ────────────────────────────────────────────────

    private fun resolveEntryContent(
        identifier: String,
        entry: PresetEntryData?,
        characterCardPrompt: String?,
        assistant: Assistant,
        userPersona: String,
        macroContext: TavernMacroContext,
    ): String {
        return when (identifier) {
            "main" -> resolveMainContent(entry, characterCardPrompt, assistant, macroContext)
            "jailbreak" -> resolveJailbreakContent(entry, assistant, macroContext)
            "chardescription" -> resolveMarkerContent(entry, assistant.description, macroContext)
            "charpersonality" -> resolveMarkerContent(entry, assistant.personality, macroContext)
            "scenario" -> resolveMarkerContent(entry, assistant.scenario, macroContext)
            "personadescription" -> resolveMarkerContent(entry, userPersona, macroContext)
            "enhancedefinitions", "nsfw" -> {
                if (entry?.content?.isNotBlank() == true) {
                    resolveWithTemplates(entry.content, macroContext)
                } else ""
            }
            "dialogueexamples" -> {
                // dialogueExamples handled in the when block above, shouldn't reach here
                ""
            }
            else -> {
                // Custom identifier — use content as-is with macro resolution
                if (entry?.content?.isNotBlank() == true) {
                    resolveWithTemplates(entry.content, macroContext)
                } else ""
            }
        }.let { resolved ->
            // Final pass for any remaining macros
            if (resolved.isNotBlank()) {
                val secondPass = resolveWithTemplates(resolved, macroContext)
                secondPass
            } else ""
        }
    }

    private fun resolveMainContent(
        entry: PresetEntryData?,
        @Suppress("UNUSED_PARAMETER") characterCardPrompt: String?,
        assistant: Assistant,
        macroContext: TavernMacroContext,
    ): String {
        val rawContent = if (entry != null && entry.forbidOverrides) {
            // forbidOverrides → always use entry content
            entry.content
        } else {
            // Check for mainPromptOverride
            val entryContent = entry?.content.orEmpty()
            if (assistant.mainPromptOverride.isNotBlank()) {
                resolveWithTemplates(
                    text = assistant.mainPromptOverride,
                    tavernCtx = macroContext.copy(original = entryContent),
                )
            } else {
                entryContent
            }
        }

        return resolveWithTemplates(rawContent, macroContext)
    }

    private fun resolveJailbreakContent(
        entry: PresetEntryData?,
        assistant: Assistant,
        macroContext: TavernMacroContext,
    ): String {
        if (entry?.forbidOverrides == true) {
            return resolveWithTemplates(entry.content, macroContext)
        }

        // Override with postHistoryInstructions if available
        if (assistant.postHistoryInstructions.isNotBlank()) {
            val entryContent = entry?.content.orEmpty()
            return resolveWithTemplates(
                text = assistant.postHistoryInstructions,
                tavernCtx = macroContext.copy(original = entryContent),
            )
        }

        return entry?.content?.let {
            resolveWithTemplates(it, macroContext)
        }.orEmpty()
    }

    private fun resolveMarkerContent(
        entry: PresetEntryData?,
        liveData: String,
        macroContext: TavernMacroContext,
    ): String {
        return if (entry?.forbidOverrides == true) {
            entry.content.let { resolveWithTemplates(it, macroContext) }
        } else if (entry?.marker == true) {
            if (liveData.isNotBlank()) {
                resolveWithTemplates(liveData, macroContext)
            } else {
                ""
            }
        } else {
            entry?.content?.let {
                resolveWithTemplates(it, macroContext)
            }.orEmpty()
        }
    }

    private fun resolveDialogueExamples(
        entry: PresetEntryData?,
        macroContext: TavernMacroContext,
    ): UIMessage? {
        if (entry?.forbidOverrides == true) {
            val content = resolveWithTemplates(entry.content, macroContext)
            if (content.isBlank()) return null
            return UIMessage.system(prompt = content)
        }

        if (entry?.marker == true) {
            // Resolve {{mesExamples}} from character card
            val examples = resolveWithTemplates("{{mesExamples}}", macroContext)
            if (examples.isBlank()) return null
            return UIMessage.system(prompt = examples)
        }

        val content = entry?.content?.let {
            resolveWithTemplates(it, macroContext)
        } ?: return null
        if (content.isBlank()) return null
        return UIMessage.system(prompt = content)
    }

    // ─── World info content resolution ──────────────────────────────────────────

    /**
     * Resolve world book content: apply regex scripts, then macro resolution.
     * Returns the resolved text string, or null if blank.
     */
    private fun resolveWorldInfoText(
        content: String,
        depth: Int,
        regexScripts: List<TavernRegexScript>,
        macroContext: TavernMacroContext,
    ): String? {
        val regexed = TavernRegexRunner.apply(
            text = content,
            scripts = regexScripts,
            placement = TavernRegexPlacement.WORLD_INFO,
            macroContext = macroContext,
            isPrompt = true,
            depth = depth,
        )
        val resolved = resolveWithTemplates(regexed, macroContext)
        return resolved.takeIf { it.isNotBlank() }
    }

    /**
     * Resolve world book content into a UIMessage, or null if blank.
     */
    private fun resolveWorldInfoContent(
        content: String,
        role: String,
        depth: Int,
        regexScripts: List<TavernRegexScript>,
        macroContext: TavernMacroContext,
    ): UIMessage? {
        val text = resolveWorldInfoText(content, depth, regexScripts, macroContext) ?: return null
        return when (role.lowercase()) {
            "user" -> UIMessage.user(text)
            "assistant" -> UIMessage.assistant(text)
            else -> UIMessage.system(prompt = text)
        }
    }
}
