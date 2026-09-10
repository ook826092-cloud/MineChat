package cn.mine.minestars.data.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import cn.mine.ai.core.MessageRole
import cn.mine.ai.core.ReasoningLevel
import cn.mine.ai.core.Tool
import cn.mine.ai.core.merge
import cn.mine.ai.provider.CustomBody
import cn.mine.ai.provider.Model
import cn.mine.ai.provider.Provider
import cn.mine.ai.provider.ProviderManager
import cn.mine.ai.provider.ProviderSetting
import cn.mine.ai.provider.TextGenerationParams
import cn.mine.ai.registry.ModelRegistry
import cn.mine.ai.ui.UIMessage
import cn.mine.ai.ui.UIMessagePart
import cn.mine.ai.ui.ToolApprovalState
import cn.mine.ai.ui.handleMessageChunk

import cn.mine.minestars.core.tavern.macro.TavernMacroContext
import cn.mine.minestars.core.tavern.macro.TavernMacroResolver
import cn.mine.minestars.core.tavern.macro.TemplateMacroContext
import cn.mine.minestars.core.tavern.macro.TemplateMacroEngine
import cn.mine.minestars.core.tavern.macro.TemplateMacroVariableStore
import cn.mine.minestars.data.ai.transformers.InputMessageTransformer
import cn.mine.minestars.data.ai.transformers.MessageTransformer
import cn.mine.minestars.data.ai.transformers.OutputMessageTransformer
import cn.mine.minestars.data.ai.transformers.onGenerationFinish
import cn.mine.minestars.data.ai.transformers.transforms
import cn.mine.minestars.data.ai.transformers.visualTransforms
import cn.mine.minestars.data.ai.tools.buildMemoryTools
import cn.mine.minestars.data.datastore.AiSettings
import cn.mine.minestars.data.datastore.AiSettingsStore
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.db.dao.ModelSelectionDAO
import cn.mine.minestars.data.db.dao.ProviderDAO
import cn.mine.minestars.data.db.dao.UserPersonaDAO
import cn.mine.minestars.data.db.findModelById
import cn.mine.minestars.data.db.findProvider
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.utils.JsonInstant
import cn.mine.minestars.data.model.AssistantMemory
import cn.mine.minestars.data.repository.ConversationRepository
import cn.mine.minestars.data.repository.MemoryRepository
import cn.mine.minestars.data.tavern.pipeline.TavernContext
import cn.mine.minestars.data.tavern.pipeline.TavernDataLoader
import cn.mine.minestars.data.tavern.pipeline.InjectionPosition
import cn.mine.minestars.data.tavern.pipeline.WorldBookInjection
import cn.mine.minestars.data.tavern.pipeline.squashSystemMessages
import cn.mine.minestars.data.tavern.pipeline.PromptOrderAssembler
import cn.mine.minestars.utils.applyPlaceholders
import java.util.Locale
import kotlin.time.Clock

private const val TAG = "GenerationHandler"

@Serializable
sealed interface GenerationChunk {
    data class Messages(
        val messages: List<UIMessage>
    ) : GenerationChunk
}

private data class TavernGenerationTransformContext(
    val regexScripts: List<cn.mine.minestars.data.tavern.pipeline.TavernRegexScript> = emptyList(),
    val macroContext: TavernMacroContext? = null,
)

class GenerationHandler(
    private val context: Context,
    private val providerManager: ProviderManager,
    private val json: Json,
    private val memoryRepo: MemoryRepository,
    private val conversationRepo: ConversationRepository,
    private val aiLoggingManager: AILoggingManager,
    private val tavernDataLoader: TavernDataLoader,
    private val providerDao: ProviderDAO,
    private val modelSelectionDAO: ModelSelectionDAO,
    private val userPersonaDao: UserPersonaDAO,
    private val aiSettingsStore: AiSettingsStore,
    private val promptOrderAssembler: PromptOrderAssembler = PromptOrderAssembler(),
) {
    private suspend fun resolveProviders(): List<ProviderSetting> {
        return providerDao.getAll().map { entity ->
            JsonInstant.decodeFromString(entity.config)
        }
    }

    private suspend fun resolveUserPersona(assistant: Assistant): UserPersonaInfo {
        if (assistant.userPersonaId == null) return UserPersonaInfo.DEFAULT
        val entity = userPersonaDao.getById(assistant.userPersonaId.toString())
        if (entity == null) return UserPersonaInfo.DEFAULT
        return UserPersonaInfo(
            name = entity.name.ifBlank { "user" },
            description = entity.description,
        )
    }

    private data class UserPersonaInfo(
        val name: String = "user",
        val description: String = "",
    ) {
        companion object {
            val DEFAULT = UserPersonaInfo()
        }
    }

    fun generateText(
        settings: Settings,
        model: Model,
        messages: List<UIMessage>,
        inputTransformers: List<InputMessageTransformer> = emptyList(),
        outputTransformers: List<OutputMessageTransformer> = emptyList(),
        assistant: Assistant,
        memories: List<AssistantMemory>? = null,
        tools: List<Tool> = emptyList(),
        maxSteps: Int = 256,
        processingStatus: MutableStateFlow<String?> = MutableStateFlow(null),
        conversationSystemPrompt: String? = null,
    ): Flow<GenerationChunk> = flow {
        val providers = resolveProviders()
        val provider = model.findProvider(providers) ?: error("Provider not found")
        val providerImpl = providerManager.getProviderByType(provider)

        var messages: List<UIMessage> = messages
        var tavernTransformContext = TavernGenerationTransformContext()

        for (stepIndex in 0 until maxSteps) {
            Log.i(TAG, "streamText: start step #$stepIndex (${model.id})")

            val toolsInternal = buildList {
                Log.i(TAG, "generateInternal: build tools($assistant)")
                if (assistant.enableMemory) {
                    val memoryAssistantId = if (assistant.useGlobalMemory) {
                        MemoryRepository.GLOBAL_MEMORY_ID
                    } else {
                        assistant.id.toString()
                    }
                    buildMemoryTools(
                        json = json,
                        onCreation = { content ->
                            memoryRepo.addMemory(memoryAssistantId, content)
                        },
                        onUpdate = { id, content ->
                            memoryRepo.updateContent(id, content)
                        },
                        onDelete = { id ->
                            memoryRepo.deleteMemory(id)
                        }
                    ).let(this::addAll)
                }
                addAll(tools)
            }

            // Check if we have tool calls ready to continue after user interaction.
            val pendingTools = messages.lastOrNull()?.getTools()?.filter {
                it.canResumeExecution
            } ?: emptyList()

            val toolsToProcess: List<UIMessagePart.Tool>

            // Skip generation if we have approved/denied tool calls to handle
            if (pendingTools.isEmpty()) {
                generateInternal(
                    assistant = assistant,
                    settings = settings,
                    messages = messages,
                    onTavernContextReady = { tavernTransformContext = it },
                    onUpdateMessages = {
                        messages = it.transforms(
                            transformers = outputTransformers,
                            context = context,
                            model = model,
                            assistant = assistant,
                            settings = settings,
                            tavernRegexScripts = tavernTransformContext.regexScripts,
                            tavernMacroContext = tavernTransformContext.macroContext,
                        )
                        emit(
                            GenerationChunk.Messages(
                                messages.visualTransforms(
                                    transformers = outputTransformers,
                                    context = context,
                                    model = model,
                                    assistant = assistant,
                                    settings = settings,
                                    tavernRegexScripts = tavernTransformContext.regexScripts,
                                    tavernMacroContext = tavernTransformContext.macroContext,
                                )
                            )
                        )
                    },
                    transformers = inputTransformers,
                    model = model,
                    providerImpl = providerImpl,
                    provider = provider,
                    tools = toolsInternal,
                    memories = memories ?: emptyList(),
                    processingStatus = processingStatus,
                    conversationSystemPrompt = conversationSystemPrompt,
                )
                messages = messages.visualTransforms(
                    transformers = outputTransformers,
                    context = context,
                    model = model,
                    assistant = assistant,
                    settings = settings,
                    tavernRegexScripts = tavernTransformContext.regexScripts,
                    tavernMacroContext = tavernTransformContext.macroContext,
                )
                messages = messages.onGenerationFinish(
                    transformers = outputTransformers,
                    context = context,
                    model = model,
                    assistant = assistant,
                    settings = settings,
                    tavernRegexScripts = tavernTransformContext.regexScripts,
                    tavernMacroContext = tavernTransformContext.macroContext,
                )
                messages = messages.slice(0 until messages.lastIndex) + messages.last().copy(
                    finishedAt = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                )
                emit(GenerationChunk.Messages(messages))

                val tools = messages.last().getTools().filter { !it.isExecuted }
                if (tools.isEmpty()) {
                    // no tool calls, break
                    break
                }

                // Check for tools that need approval
                var hasPendingApproval = false
                val updatedTools = tools.map { tool ->
                    val toolDef = toolsInternal.find { it.name == tool.toolName }
                    when {
                        // Tool needs approval and state is Auto -> set to Pending
                        toolDef?.needsApproval == true && tool.approvalState is ToolApprovalState.Auto -> {
                            hasPendingApproval = true
                            tool.copy(approvalState = ToolApprovalState.Pending)
                        }
                        // State is Pending -> keep waiting
                        tool.approvalState is ToolApprovalState.Pending -> {
                            hasPendingApproval = true
                            tool
                        }

                        else -> tool
                    }
                }

                // If any tools were updated to Pending, update the message and break
                if (updatedTools != tools) {
                    val lastMessage = messages.last()
                    val updatedParts = lastMessage.parts.map { part ->
                        if (part is UIMessagePart.Tool) {
                            updatedTools.find { it.toolCallId == part.toolCallId } ?: part
                        } else {
                            part
                        }
                    }
                    messages = messages.dropLast(1) + lastMessage.copy(parts = updatedParts)
                    emit(GenerationChunk.Messages(messages))
                }

                // If there are pending approvals, break and wait for user
                if (hasPendingApproval) {
                    Log.i(TAG, "generateText: waiting for tool approval")
                    break
                }

                toolsToProcess = updatedTools
            } else {
                // Resuming after user interaction - use the resumable tools directly.
                Log.i(TAG, "generateText: resuming with ${pendingTools.size} resumable tools")
                toolsToProcess = messages.last().getTools().filter { it.canResumeExecution }
            }

            // Handle tools (execute approved tools, handle denied tools)
            val executedTools = arrayListOf<UIMessagePart.Tool>()
            toolsToProcess.forEach { tool ->
                when (tool.approvalState) {
                    is ToolApprovalState.Denied -> {
                        // Tool was denied by user
                        val reason = (tool.approvalState as ToolApprovalState.Denied).reason
                        executedTools += tool.copy(
                            output = listOf(
                                UIMessagePart.Text(
                                    json.encodeToString(
                                        buildJsonObject {
                                            put(
                                                "error",
                                                JsonPrimitive("Tool execution denied by user. Reason: ${reason.ifBlank { "No reason provided" }}")
                                            )
                                        }
                                    )
                                )
                            )
                        )
                    }

                    is ToolApprovalState.Answered -> {
                        // Tool was answered by user (e.g., ask_user tool)
                        val answer = (tool.approvalState as ToolApprovalState.Answered).answer
                        executedTools += tool.copy(
                            output = listOf(
                                UIMessagePart.Text(answer)
                            )
                        )
                    }

                    is ToolApprovalState.Pending -> {
                        // Should not reach here, but just in case
                    }

                    else -> {
                        // Auto or Approved - execute the tool
                        runCatching {
                            val toolDef = toolsInternal.find { toolDef -> toolDef.name == tool.toolName }
                                ?: error("Tool ${tool.toolName} not found")
                            val args = runCatching {
                                json.parseToJsonElement(tool.input.ifBlank { "{}" })
                            }.getOrElse {
                                error("Invalid tool arguments JSON for ${tool.toolName}: ${it.message}")
                            }
                            Log.i(TAG, "generateText: executing tool ${toolDef.name} with args: $args")
                            val result = toolDef.execute(args)
                            executedTools += tool.copy(output = result)
                        }.onFailure {
                            it.printStackTrace()
                            executedTools += tool.copy(
                                output = listOf(
                                    UIMessagePart.Text(
                                        json.encodeToString(
                                            buildJsonObject {
                                                put(
                                                    "error",
                                                    JsonPrimitive(buildString {
                                                        append("[${it.javaClass.name}] ${it.message}")
                                                        append("\n${it.stackTraceToString()}")
                                                    })
                                                )
                                            }
                                        )
                                    )
                                )
                            )
                        }
                    }
                }
            }

            if (executedTools.isEmpty()) {
                // No results to add (all tools were pending)
                break
            }

            // Update last message with executed tools (NOT create TOOL message)
            val lastMessage = messages.last()
            val updatedParts = lastMessage.parts.map { part ->
                if (part is UIMessagePart.Tool) {
                    executedTools.find { it.toolCallId == part.toolCallId } ?: part
                } else part
            }
            messages = messages.dropLast(1) + lastMessage.copy(parts = updatedParts)
            emit(
                GenerationChunk.Messages(
                    messages.transforms(
                        transformers = outputTransformers,
                        context = context,
                        model = model,
                        assistant = assistant,
                        settings = settings
                    )
                )
            )
        }

    }.flowOn(Dispatchers.IO)

    private suspend fun generateInternal(
        assistant: Assistant,
        settings: Settings,
        messages: List<UIMessage>,
        onTavernContextReady: (TavernGenerationTransformContext) -> Unit = {},
        onUpdateMessages: suspend (List<UIMessage>) -> Unit,
        transformers: List<MessageTransformer>,
        model: Model,
        providerImpl: Provider<ProviderSetting>,
        provider: ProviderSetting,
        tools: List<Tool>,
        memories: List<AssistantMemory>,
        processingStatus: MutableStateFlow<String?> = MutableStateFlow(null),
        conversationSystemPrompt: String? = null,
    ) {
        // Resolve user persona from Room database (per-assistant)
        val userPersona = resolveUserPersona(assistant)

        // Phase 1: Build macro context EARLY — loadContext needs it for regex + character card macro resolution
        val macroContext = buildTavernMacroContext(
            assistant = assistant,
            settings = settings,
            model = model,
            userPersona = userPersona,
        )

        // Phase 2: Load tavern data (preset, regex, WI, character card)
        // Returns Pair(tavernCtx, regexdMessages) where regexdMessages have USER_INPUT/AI_OUTPUT
        // regex applied BEFORE WI scan, matching SillyTavern's order of operations.
        val hasTavernData = assistant.charName.isNotBlank() ||
            assistant.presetId != null ||
            assistant.worldBookIds.isNotEmpty() ||
            assistant.regexGroupIds.isNotEmpty()
        val (tavernCtx, regexdMessages) = if (hasTavernData) {
            try {
                tavernDataLoader.loadContext(
                    assistant = assistant,
                    messages = messages,
                    userName = userPersona.name,
                    tavernMacroContext = macroContext,
                )
            } catch (_: Exception) {
                null to messages
            }
        } else null to messages

        val stream = tavernCtx?.presetModelParams?.streamOutput ?: true

        // Phase 3: Build TemplateMacroContext AFTER loadContext so userInput uses regex'd messages
        val templateContext = buildTemplateMacroContext(
            assistant = assistant,
            settings = settings,
            model = model,
            userPersona = userPersona,
            userInput = regexdMessages.lastOrNull()?.toText().orEmpty(),
            messages = regexdMessages,
            tools = tools,
            maxContextTokens = tavernCtx?.presetModelParams?.maxContext ?: 8192,
            maxResponseTokens = tavernCtx?.presetModelParams?.maxTokens ?: 1024,
        )
        onTavernContextReady(
            TavernGenerationTransformContext(
                regexScripts = tavernCtx?.regexScripts ?: emptyList(),
                macroContext = macroContext,
            )
        )

        // Build supplement messages for memories, tools, recent chats
        val supplementMessages = buildList {
            if (assistant.enableMemory) {
                val memoryPrompt = buildMemoryPrompt(memories = memories)
                if (memoryPrompt.isNotBlank()) add(UIMessage.system(prompt = memoryPrompt))
            }
            if (assistant.enableRecentChatsReference) {
                val recentChatsPrompt = buildRecentChatsPrompt(assistant, conversationRepo)
                if (recentChatsPrompt.isNotBlank()) add(UIMessage.system(prompt = recentChatsPrompt))
            }
            tools.forEach { tool ->
                val toolPrompt = tool.systemPrompt(model, messages)
                if (toolPrompt.isNotBlank()) add(UIMessage.system(prompt = toolPrompt))
            }
        }

        // Phase 4: (removed — context message truncation was removed as it breaks prompt caching)

        // Phase 5: Prompt order assembly using REGEX'D messages
        val internalMessagesRaw = if (tavernCtx?.promptOrder != null && tavernCtx.promptOrder.isNotEmpty()) {
            Log.d(TAG, "generateInternal: prompt_order assembly (${tavernCtx.promptOrder.size} items)")

            promptOrderAssembler.assemble(
                promptOrder = tavernCtx.promptOrder,
                presetEntries = tavernCtx.presetEntries,
                characterCardPrompt = tavernCtx.characterCardPrompt,
                assistant = assistant,
                userPersona = userPersona.description,
                worldBookBefore = tavernCtx.worldBookInjections
                    .filter { it.position == InjectionPosition.BEFORE_PROMPT },
                worldBookAfter = tavernCtx.worldBookInjections
                    .filter { it.position == InjectionPosition.AFTER_PROMPT },
                worldBookDepth = tavernCtx.worldBookInjections
                    .filter { it.position == InjectionPosition.AT_DEPTH },
                historyMessages = regexdMessages,
                macroContext = macroContext,
                regexScripts = tavernCtx.regexScripts,
                supplementMessages = supplementMessages,
                templateMacroContext = templateContext,
            )
        } else {
            if (tavernCtx != null) {
                Log.d(TAG, "generateInternal: no prompt_order in preset, using bare messages")
            } else {
                Log.d(TAG, "generateInternal: no tavern context, using bare messages")
            }

            // No prompt_order available — just append supplement messages + history
            buildList {
                if (supplementMessages.isNotEmpty()) {
                    addAll(supplementMessages)
                }
                addAll(regexdMessages)
            }
        }

        // Phase 6: Squash consecutive system messages if enabled in preset
        // (ST's squash_system_messages toggle, defaults to false)
        val internalMessagesSquashed = if (tavernCtx?.squashSystemMessages == true) {
            internalMessagesRaw.squashSystemMessages()
        } else internalMessagesRaw

        // Log the full assembled message list for debugging
        Log.d(TAG, "=== internalMessagesSquashed (${internalMessagesSquashed.size} msgs) ===")
        internalMessagesSquashed.forEachIndexed { i, msg ->
            val text = msg.toText()
            val preview = text.take(200).replace("\n", "\\n")
            Log.d(TAG, "  [$i] role=${msg.role} len=${text.length} preview='$preview'")
        }

        // Phase 7: Apply input transformers
        val internalMessages = internalMessagesSquashed.transforms(
            transformers = transformers,
            context = context,
            model = model,
            assistant = assistant,
            settings = settings,
            processingStatus = processingStatus,
            tavernRegexScripts = tavernCtx?.regexScripts ?: emptyList(),
            tavernMacroContext = macroContext,
        )

        var messages: List<UIMessage> = messages
        val presetParams = tavernCtx?.presetModelParams
        val params = TextGenerationParams(
            model = model,
            temperature = presetParams?.temperature,
            topP = presetParams?.topP,
            topK = presetParams?.topK,
            minP = presetParams?.minP,
            topA = presetParams?.topA,
            repetitionPenalty = presetParams?.repetitionPenalty,
            frequencyPenalty = presetParams?.frequencyPenalty,
            presencePenalty = presetParams?.presencePenalty,
            seed = presetParams?.seed,
            n = presetParams?.n,
            maxTokens = presetParams?.maxTokens,
            stop = presetParams?.stop ?: emptyList(),
            tools = tools,
            reasoningLevel = try {
                ReasoningLevel.valueOf((presetParams?.reasoningLevel ?: "auto").uppercase())
            } catch (_: Exception) {
                ReasoningLevel.AUTO
            },
            // Convert Int (ST format, from preset) to String (TextGenerationParams format)
            characterNamesBehavior = when (presetParams?.characterNamesBehavior) {
                null -> "default"
                -1 -> "none"
                0 -> "default"
                1 -> "completion"
                2 -> "content"
                else -> "default"
            },
            // Merge assistant-level custom headers/bodies on top of model-level values,
            // with assistant values overriding model ones when names/keys conflict.
            customHeaders = if (assistant.customHeaders.isEmpty()) model.customHeaders
                else model.customHeaders.filter { m -> assistant.customHeaders.none { it.name == m.name } } + assistant.customHeaders,
            customBody = if (assistant.customBodies.isEmpty()) model.customBodies
                else model.customBodies.filter { m -> assistant.customBodies.none { it.key == m.key } } + assistant.customBodies,
        )
        if (stream) {
            aiLoggingManager.addLog(
                AILogging.Generation(
                    params = params,
                    messages = internalMessages,
                    providerSetting = provider,
                    stream = true
                )
            )
            providerImpl.streamText(
                providerSetting = provider,
                messages = internalMessages,
                params = params
            ).collect {
                messages = messages.handleMessageChunk(chunk = it, model = model)
                it.usage?.let { usage ->
                    messages = messages.mapIndexed { index, message ->
                        if (index == messages.lastIndex) {
                            message.copy(usage = message.usage.merge(usage))
                        } else {
                            message
                        }
                    }
                }
                onUpdateMessages(messages)
            }
        } else {
            aiLoggingManager.addLog(
                AILogging.Generation(
                    params = params,
                    messages = internalMessages,
                    providerSetting = provider,
                    stream = false
                )
            )
            val chunk = providerImpl.generateText(
                providerSetting = provider,
                messages = internalMessages,
                params = params,
            )
            messages = messages.handleMessageChunk(chunk = chunk, model = model)
            chunk.usage?.let { usage ->
                messages = messages.mapIndexed { index, message ->
                    if (index == messages.lastIndex) {
                        message.copy(
                            usage = message.usage.merge(usage)
                        )
                    } else {
                        message
                    }
                }
            }
            onUpdateMessages(messages)
        }
    }

    private fun buildTavernMacroContext(
        assistant: Assistant,
        settings: Settings,
        model: Model,
        userPersona: UserPersonaInfo,
    ): TavernMacroContext {
        val charName = assistant.charName.ifBlank { assistant.name.ifBlank { "assistant" } }
        return TavernMacroContext(
            userName = userPersona.name,
            charName = charName,
            group = charName,
            groupNotMuted = charName,
            notChar = userPersona.name,
            model = model.displayName.ifBlank { model.modelId },
            description = assistant.description,
            personality = assistant.personality,
            scenario = assistant.scenario,
            persona = userPersona.description,
            mesExamplesRaw = assistant.mesExamples,
            firstMessage = assistant.firstMessage,
            alternateGreetings = assistant.alternateGreetings,
            mainPromptOverride = assistant.mainPromptOverride,
            postHistoryInstructions = assistant.postHistoryInstructions,
            creatorNotes = assistant.creatorNotes,
            characterVersion = assistant.characterVersion,
            isMobile = true,
        )
    }

    /**
     * Build a [TemplateMacroContext] for setvar/getvar support.
     * Variables are backed by [TemplateMacroVariableStore] (in-memory, assistant-scoped).
     */
    private fun buildTemplateMacroContext(
        assistant: Assistant,
        settings: Settings,
        model: Model,
        userPersona: UserPersonaInfo,
        userInput: String,
        messages: List<UIMessage> = emptyList(),
        tools: List<Tool> = emptyList(),
        maxContextTokens: Int = 8192,
        maxResponseTokens: Int = 1024,
    ): TemplateMacroContext {
        val charName = assistant.charName.ifBlank { assistant.name.ifBlank { "assistant" } }
        val userPersonaDesc = userPersona.description

        // Derive message reference data
        val lastMessageId = messages.size - 1
        val lastMessage = messages.lastOrNull()?.toText().orEmpty()

        val lastUserMessageId = messages.indexOfLast { it.role == MessageRole.USER }
        val lastUserMessage = if (lastUserMessageId >= 0) messages[lastUserMessageId].toText() else ""

        val lastCharMessageId = messages.indexOfLast { it.role == MessageRole.ASSISTANT }
        val lastCharMessage = if (lastCharMessageId >= 0) messages[lastCharMessageId].toText() else ""

        // Last message timestamp for idle_duration (best effort)
        val lastMessageTime = messages.lastOrNull()
            ?.let { msg ->
                // Try to extract timestamp if available
                msg.finishedAt?.let { ft -> ft.toString() }
            }.orEmpty()

        return TemplateMacroContext(
            builtins = mapOf(
                "user" to userPersona.name,
                "char" to charName,
                "character" to charName,
                "assistant" to charName,
                "name1" to userPersona.name,
                "name2" to charName,
                "group" to charName,
                "groupnotmuted" to charName,
                "notchar" to userPersona.name,
                "description" to assistant.description,
                "chardescription" to assistant.description,
                "personality" to assistant.personality,
                "charpersonality" to assistant.personality,
                "scenario" to assistant.scenario,
                "charscenario" to assistant.scenario,
                "persona" to userPersonaDesc,
                "personadescription" to userPersonaDesc,
                "user_description" to userPersonaDesc,
                "mesExamples" to assistant.mesExamples,
                "mesexamplesraw" to assistant.mesExamples,
                "firstMessage" to assistant.firstMessage,
                "mainPromptOverride" to assistant.mainPromptOverride,
                "postHistoryInstructions" to assistant.postHistoryInstructions,
                "creatorNotes" to assistant.creatorNotes,
                "characterVersion" to assistant.characterVersion,
                "model" to model.displayName.ifBlank { model.modelId },
                "ismobile" to "true",
                "input" to userInput,
                // ── Message reference macros ────────────────────────────────
                "lastmessage" to lastMessage,
                "lastmessageid" to lastMessageId.toString(),
                "lastusermessage" to lastUserMessage,
                "lastcharmessage" to lastCharMessage,
                "lastusermessageid" to (if (lastUserMessageId >= 0) lastUserMessageId.toString() else ""),
                "lastcharmessageid" to (if (lastCharMessageId >= 0) lastCharMessageId.toString() else ""),
                "firstincludedmessageid" to "0",
                "firstdisplayedmessageid" to "0",
                "lastswipeid" to "",
                "currentswipeid" to "",
                "lastgenerationtype" to "normal",
                // ── Context size macros ─────────────────────────────────────
                "maxcontext" to maxContextTokens.toString(),
                "maxcontexttokens" to maxContextTokens.toString(),
                "maxresponse" to maxResponseTokens.toString(),
                "maxresponsetokens" to maxResponseTokens.toString(),
                "allchatrange" to "0-${lastMessageId.coerceAtLeast(0)}",
                // ── idle_duration support ───────────────────────────────────
                "lastmessagetime" to lastMessageTime,
                "idleduration" to "",
                "idle_duration" to "",
            ),
            variables = TemplateMacroVariableStore.assistantScope(assistant.id.toString()),
            globalVariables = TemplateMacroVariableStore.globalScope(),
            extensions = tools.filter { it.name.isNotBlank() }.map { it.name }.toSet(),
        )
    }

    fun translateText(
        sourceText: String,
        targetLanguage: Locale,
        onStreamUpdate: ((String) -> Unit)? = null
    ): Flow<String> = flow {
        val aiSettings = aiSettingsStore.flow.first()
        val providers = resolveProviders()
        val translateModelId = modelSelectionDAO.get()?.translateModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() }
        val model = translateModelId?.let { providers.findModelById(it) }
            ?: error("Translation model not found")
        val provider = model.findProvider(providers)
            ?: error("Translation provider not found")

        val providerHandler = providerManager.getProviderByType(provider)

        if (!ModelRegistry.QWEN_MT.match(model.modelId)) {
            // Use regular translation with prompt
            val prompt = aiSettings.translatePrompt.applyPlaceholders(
                "source_text" to sourceText,
                "target_lang" to targetLanguage.toString(),
            )

            var messages = listOf(UIMessage.user(prompt))
            var translatedText = ""

            providerHandler.streamText(
                providerSetting = provider,
                messages = messages,
                params = TextGenerationParams(
                    model = model,
                    reasoningLevel = ReasoningLevel.fromBudgetTokens(aiSettings.translateThinkingBudget),
                ),
            ).collect { chunk ->
                messages = messages.handleMessageChunk(chunk)
                translatedText = messages.lastOrNull()?.toText() ?: ""

                if (translatedText.isNotBlank()) {
                    onStreamUpdate?.invoke(translatedText)
                    emit(translatedText)
                }
            }
        } else {
            // Use Qwen MT model with special translation options
            val messages = listOf(UIMessage.user(sourceText))
            val chunk = providerHandler.generateText(
                providerSetting = provider,
                messages = messages,
                params = TextGenerationParams(
                    model = model,
                    temperature = 0.3f,
                    topP = 0.95f,
                    customBody = listOf(
                        CustomBody(
                            key = "translation_options",
                            value = buildJsonObject {
                                put("source_lang", JsonPrimitive("auto"))
                                put(
                                    "target_lang",
                                    JsonPrimitive(targetLanguage.getDisplayLanguage(Locale.ENGLISH))
                                )
                            }
                        )
                    )
                ),
            )
            val translatedText = chunk.choices.firstOrNull()?.message?.toText() ?: ""

            if (translatedText.isNotBlank()) {
                onStreamUpdate?.invoke(translatedText)
                emit(translatedText)
            }
        }
    }.flowOn(Dispatchers.IO)
}
