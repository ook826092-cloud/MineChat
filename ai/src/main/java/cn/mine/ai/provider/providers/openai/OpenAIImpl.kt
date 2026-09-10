package cn.mine.ai.provider.providers.openai

import kotlinx.coroutines.flow.Flow
import cn.mine.ai.provider.ProviderSetting
import cn.mine.ai.provider.TextGenerationParams
import cn.mine.ai.ui.MessageChunk
import cn.mine.ai.ui.UIMessage

interface OpenAIImpl {
    suspend fun generateText(
        providerSetting: ProviderSetting.OpenAI,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): MessageChunk

    suspend fun streamText(
        providerSetting: ProviderSetting.OpenAI,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): Flow<MessageChunk>
}
