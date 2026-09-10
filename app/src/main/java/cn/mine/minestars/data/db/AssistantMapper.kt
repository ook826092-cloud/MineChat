package cn.mine.minestars.data.db

import cn.mine.ai.provider.CustomBody
import cn.mine.ai.provider.CustomHeader
import cn.mine.minestars.data.ai.tools.LocalToolOption
import cn.mine.minestars.data.db.entity.AssistantEntity
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.data.model.Avatar
import cn.mine.minestars.utils.JsonInstant
import kotlin.uuid.Uuid

fun AssistantEntity.toModel(): Assistant {
    return Assistant(
        id = Uuid.parse(this.id),
        name = this.name,
        chatModelId = this.chatModelId?.let { runCatching { Uuid.parse(it) }.getOrNull() },
        knowledgeBaseId = this.knowledgeBaseId?.let { runCatching { Uuid.parse(it) }.getOrNull() },
        presetId = this.presetId?.let { runCatching { Uuid.parse(it) }.getOrNull() },
        userPersonaId = this.userPersonaId?.let { runCatching { Uuid.parse(it) }.getOrNull() },
        avatar = runCatching {
            JsonInstant.decodeFromString<Avatar>(this.avatar)
        }.getOrDefault(Avatar.Dummy),
        useAssistantAvatar = this.useAssistantAvatar,
        enableMemory = this.enableMemory,
        useGlobalMemory = this.useGlobalMemory,
        enableRecentChatsReference = this.enableRecentChatsReference,
        messageTemplate = this.messageTemplate,
        quickMessageIds = runCatching {
            JsonInstant.decodeFromString<Set<Uuid>>(this.quickMessageIds)
        }.getOrDefault(emptySet()),
        localTools = runCatching {
            JsonInstant.decodeFromString<List<LocalToolOption>>(this.localTools)
        }.getOrDefault(emptyList()),
        background = this.background,
        backgroundOpacity = this.backgroundOpacity,
        enabledSkills = runCatching {
            JsonInstant.decodeFromString<Set<String>>(this.enabledSkills)
        }.getOrDefault(emptySet()),
        enableTimeReminder = this.enableTimeReminder,
        allowConversationSystemPrompt = this.allowConversationSystemPrompt,
        interleavedThinking = this.interleavedThinking,
        charName = this.charName,
        description = this.description,
        personality = this.personality,
        scenario = this.scenario,
        firstMessage = this.firstMessage,
        mesExamples = this.mesExamples,
        mainPromptOverride = this.mainPromptOverride,
        postHistoryInstructions = this.postHistoryInstructions,
        creatorNotes = this.creatorNotes,
        alternateGreetings = runCatching {
            JsonInstant.decodeFromString<List<String>>(this.alternateGreetings)
        }.getOrDefault(emptyList()),
        groupOnlyGreetings = runCatching {
            JsonInstant.decodeFromString<List<String>>(this.groupOnlyGreetings)
        }.getOrDefault(emptyList()),
        nickname = this.nickname,
        cardTags = runCatching {
            JsonInstant.decodeFromString<List<String>>(this.cardTags)
        }.getOrDefault(emptyList()),
        creator = this.creator,
        characterVersion = this.characterVersion,
        source = this.source,
        tags = emptyList(),
        mcpServers = runCatching {
            JsonInstant.decodeFromString<Set<Uuid>>(this.mcpServerIds)
        }.getOrDefault(emptySet()),
        worldBookIds = runCatching {
            JsonInstant.decodeFromString<Set<Uuid>>(this.worldBookIds ?: "[]")
        }.getOrDefault(emptySet()),
        regexGroupIds = runCatching {
            JsonInstant.decodeFromString<Set<Uuid>>(this.regexGroupIds ?: "[]")
        }.getOrDefault(emptySet()),
        worldBookJson = this.worldBookJson,
        regexScriptsJson = this.regexScriptsJson,
        customHeaders = runCatching {
            JsonInstant.decodeFromString<List<CustomHeader>>(this.customHeaders ?: "[]")
        }.getOrDefault(emptyList()),
        customBodies = runCatching {
            JsonInstant.decodeFromString<List<CustomBody>>(this.customBodies ?: "[]")
        }.getOrDefault(emptyList()),
        workspaceId = this.workspaceId,
    )
}

fun Assistant.toEntity(): AssistantEntity {
    val now = System.currentTimeMillis()
    return AssistantEntity(
        id = this.id.toString(),
        name = this.name,
        chatModelId = this.chatModelId?.toString(),
        characterCardId = null,
        knowledgeBaseId = this.knowledgeBaseId?.toString(),
        presetId = this.presetId?.toString(),
        userPersonaId = this.userPersonaId?.toString(),
        avatar = JsonInstant.encodeToString(this.avatar),
        regexGroupIds = JsonInstant.encodeToString(this.regexGroupIds),
        worldBookIds = JsonInstant.encodeToString(this.worldBookIds),
        worldBookJson = this.worldBookJson,
        regexScriptsJson = this.regexScriptsJson,
        customHeaders = JsonInstant.encodeToString(this.customHeaders),
        customBodies = JsonInstant.encodeToString(this.customBodies),
        workspaceId = this.workspaceId,
        mcpServerIds = JsonInstant.encodeToString(this.mcpServers),
        useAssistantAvatar = this.useAssistantAvatar,
        enableMemory = this.enableMemory,
        useGlobalMemory = this.useGlobalMemory,
        enableRecentChatsReference = this.enableRecentChatsReference,
        messageTemplate = this.messageTemplate,
        quickMessageIds = JsonInstant.encodeToString(this.quickMessageIds),
        localTools = JsonInstant.encodeToString(this.localTools),
        background = this.background,
        backgroundOpacity = this.backgroundOpacity,
        enabledSkills = JsonInstant.encodeToString(this.enabledSkills),
        enableTimeReminder = this.enableTimeReminder,
        allowConversationSystemPrompt = this.allowConversationSystemPrompt,
        interleavedThinking = this.interleavedThinking,
        contextMessageCount = 0,
        charName = this.charName,
        description = this.description,
        personality = this.personality,
        scenario = this.scenario,
        firstMessage = this.firstMessage,
        mesExamples = this.mesExamples,
        mainPromptOverride = this.mainPromptOverride,
        postHistoryInstructions = this.postHistoryInstructions,
        creatorNotes = this.creatorNotes,
        alternateGreetings = JsonInstant.encodeToString(this.alternateGreetings),
        groupOnlyGreetings = JsonInstant.encodeToString(this.groupOnlyGreetings),
        nickname = this.nickname,
        cardTags = JsonInstant.encodeToString(this.cardTags),
        creator = this.creator,
        characterVersion = this.characterVersion,
        source = this.source,
        displayOrder = 0,
        createdAt = now,
        updatedAt = now,
    )
}

fun List<AssistantEntity>.toModels(): List<Assistant> {
    return map { it.toModel() }
}
