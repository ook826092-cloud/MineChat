package cn.mine.minestars.data.model

import cn.mine.ai.provider.CustomBody
import cn.mine.ai.provider.CustomHeader
import kotlinx.serialization.Serializable
import cn.mine.minestars.data.ai.tools.LocalToolOption
import kotlin.uuid.Uuid

@Serializable
data class Assistant(
    val id: Uuid = Uuid.random(),
    val chatModelId: Uuid? = null, // 如果为null, 使用全局默认模型
    val name: String = "",
    val avatar: Avatar = Avatar.Dummy,
    val useAssistantAvatar: Boolean = false, // 使用助手头像替代模型头像
    val tags: List<Uuid> = emptyList(),
    val enableMemory: Boolean = false,
    val useGlobalMemory: Boolean = false, // 使用全局共享记忆而非助手隔离记忆
    val enableRecentChatsReference: Boolean = false,
    val messageTemplate: String = "{{ message }}",
    val quickMessageIds: Set<Uuid> = emptySet(),
    val mcpServers: Set<Uuid> = emptySet(),
    val localTools: List<LocalToolOption> = listOf(LocalToolOption.TimeInfo),
    val useGradientBackground: Boolean = false,
    val background: String? = null,
    val backgroundOpacity: Float = 1.0f,
    val enabledSkills: Set<String> = emptySet(),        // 启用的 skill 名称列表
    val enableTimeReminder: Boolean = false,            // 时间间隔提醒注入
    val allowConversationSystemPrompt: Boolean = false, // 允许对话单独重写 system prompt
    val interleavedThinking: Boolean = false,               // 交错思维（ST tool_reasoning_mode flag）

    // Character card fields (replaces manual systemPrompt in character mode)
    val charName: String = "",          // 角色名称
    val description: String = "",       // 角色描述
    val personality: String = "",       // 性格设定
    val scenario: String = "",          // 背景故事
    val firstMessage: String = "",      // 开场白
    val mesExamples: String = "",       // 对话示例

    // Extended character card fields
    val mainPromptOverride: String = "",        // Main Prompt 覆盖（SillyTavern system_prompt）
    val postHistoryInstructions: String = "",   // 后置指令 (放在聊天记录之后)
    val creatorNotes: String = "",              // 作者附言/备注
    val alternateGreetings: List<String> = emptyList(), // 备用开场白
    val groupOnlyGreetings: List<String> = emptyList(), // 群聊开场白
    val nickname: String = "",                  // 角色昵称
    val cardTags: List<String> = emptyList(),    // 角色卡标签（字符串标签）
    val creator: String = "",                   // 作者
    val characterVersion: String = "",          // 角色版本
    val source: String = "",                    // 来源（ID 或 URL）
    // 关联的知识库 ID (RAG)
    val knowledgeBaseId: Uuid? = null,

    // Tavern binding IDs (links to TavernDatabase entities)
    val presetId: Uuid? = null,
    val userPersonaId: Uuid? = null,
    val worldBookIds: Set<Uuid> = emptySet(),
    val regexGroupIds: Set<Uuid> = emptySet(),

    // Assistant-level world book data (embedded JSON, null = none)
    val worldBookJson: String? = null,
    // Assistant-level regex scripts data (embedded JSON array, null = none)
    val regexScriptsJson: String? = null,

    // Custom HTTP headers & bodies for API requests (override/extend model-level values)
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBodies: List<CustomBody> = emptyList(),

    // Workspace binding
    val workspaceId: String? = null,

    /** 上下文消息条数上限, 超出后阶梯式截断; 0 表示不限制 */
    val contextMessageLimit: Int = 0,
)

@Serializable
data class QuickMessage(
    val id: Uuid = Uuid.random(),
    val title: String = "",
    val content: String = "",
)

@Serializable
data class AssistantMemory(
    val id: Int,
    val content: String = "",
)


