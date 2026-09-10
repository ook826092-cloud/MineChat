package cn.mine.minestars.di

import kotlinx.serialization.json.Json
import cn.mine.highlight.CodeHighlighter
import cn.mine.minestars.AppScope
import cn.mine.minestars.data.ai.AILoggingManager
import cn.mine.minestars.data.ai.tools.LocalTools
import cn.mine.minestars.data.event.AppEventBus
import cn.mine.minestars.service.ChatService
import cn.mine.minestars.utils.EmojiData
import cn.mine.minestars.utils.EmojiUtils
import cn.mine.minestars.utils.JsonInstant
import cn.mine.minestars.utils.SoundEffectPlayer
import cn.mine.minestars.utils.UpdateChecker
import cn.mine.tts.provider.TTSManager
import org.koin.dsl.module

val appModule = module {
    single<Json> { JsonInstant }

    single {
        CodeHighlighter()
    }

    single {
        AppEventBus()
    }

    single {
        LocalTools(get(), get())
    }

    single {
        UpdateChecker(get())
    }

    single {
        AppScope()
    }

    single<EmojiData> {
        EmojiUtils.loadEmoji(get())
    }

    single {
        TTSManager(get())
    }

    single {
        SoundEffectPlayer(get())
    }

    single {
        AILoggingManager()
    }

    single {
        ChatService(
            context = get(),
            appScope = get(),
            settingsStore = get(),
            aiSettingsStore = get(),
            conversationRepo = get(),
            memoryRepository = get(),
            generationHandler = get(),
            templateTransformer = get(),
            providerManager = get(),
            localTools = get(),
            mcpManager = get(),
            filesManager = get(),
            skillManager = get(),
            embeddingService = get(),
            assistantDAO = get(),
            userPersonaDAO = get(),
            providerDAO = get(),
            knowledgeBaseDAO = get(),
            searchServiceDAO = get(),
            modelSelectionDAO = get(),
            workspaceRepository = get(),
        )
    }
}
