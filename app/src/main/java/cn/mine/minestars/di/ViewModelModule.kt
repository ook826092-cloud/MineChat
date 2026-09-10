package cn.mine.minestars.di

import cn.mine.minestars.ui.pages.assistant.AssistantVM
import cn.mine.minestars.ui.pages.assistant.detail.AssistantDetailVM
import cn.mine.minestars.ui.pages.assistant.detail.AssistantTavernBindingsVM
import cn.mine.minestars.ui.pages.backup.BackupVM
import cn.mine.minestars.ui.pages.chat.ChatDrawerVM
import cn.mine.minestars.ui.pages.chat.ChatVM
import cn.mine.minestars.ui.pages.debug.DebugVM
import cn.mine.minestars.ui.pages.developer.DeveloperVM
import cn.mine.minestars.feature.character.CharacterCardVM
import cn.mine.minestars.feature.tavern.PresetDetailVM
import cn.mine.minestars.feature.tavern.PresetListVM
import cn.mine.minestars.feature.tavern.RegexGroupDetailVM
import cn.mine.minestars.feature.tavern.AssistantRegexListVM
import cn.mine.minestars.feature.tavern.RegexListVM
import cn.mine.minestars.feature.tavern.WorldBookDetailVM
import cn.mine.minestars.feature.tavern.WorldBookListVM
import cn.mine.minestars.feature.tavern.AssistantWorldBookListVM
import cn.mine.minestars.ui.pages.favorite.FavoriteVM
import cn.mine.minestars.ui.pages.search.SearchVM
import cn.mine.minestars.ui.pages.history.HistoryVM
import cn.mine.minestars.ui.pages.stats.StatsVM
import cn.mine.minestars.ui.pages.imggen.ImgGenVM
import cn.mine.minestars.ui.pages.extensions.QuickMessagesVM
import cn.mine.minestars.ui.pages.extensions.SkillDetailVM
import cn.mine.minestars.ui.pages.extensions.SkillsVM
import cn.mine.minestars.ui.pages.setting.SettingVM
import cn.mine.minestars.ui.pages.setting.UserPersonaListVM
import cn.mine.minestars.ui.pages.share.handler.ShareHandlerVM
import cn.mine.minestars.ui.pages.translator.TranslatorVM
import cn.mine.minestars.ui.pages.workspace.WorkspaceVM
import cn.mine.minestars.ui.pages.workspace.WorkspaceDetailVM
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModel<ChatVM> { params ->
        ChatVM(
            id = params.get(),
            context = get(),
            settingsStore = get(),
            aiSettingsStore = get(),
            conversationRepo = get(),
            chatService = get(),
            updateChecker = get(),
            filesManager = get(),
            favoriteRepository = get(),
            providerDAO = get(),
            assistantDAO = get(),
            modelSelectionDAO = get(),
        )
    }
    viewModelOf(::ChatDrawerVM)
    viewModelOf(::SettingVM)
    viewModelOf(::DebugVM)
    viewModelOf(::HistoryVM)
    viewModelOf(::AssistantVM)
    viewModel<AssistantDetailVM> {
        AssistantDetailVM(
            id = it.get(),
            settingsStore = get(),
            memoryRepository = get(),
            filesManager = get(),
            skillManager = get(),
            assistantDao = get(),
            mcpServerDao = get(),
            knowledgeBaseDao = get(),
            providerDao = get(),
            tagDao = get(),
        )
    }
    viewModelOf(::TranslatorVM)
    viewModel<ShareHandlerVM> {
        ShareHandlerVM(
            text = it.get(),
            settingsStore = get(),
        )
    }
    viewModelOf(::BackupVM)
    viewModelOf(::ImgGenVM)
    viewModelOf(::DeveloperVM)
    viewModelOf(::QuickMessagesVM)
    viewModelOf(::SkillsVM)
    viewModelOf(::SkillDetailVM)
    viewModelOf(::FavoriteVM)
    viewModelOf(::SearchVM)
    viewModelOf(::StatsVM)
    viewModelOf(::CharacterCardVM)
    viewModelOf(::AssistantTavernBindingsVM)
    viewModelOf(::PresetListVM)
    viewModelOf(::PresetDetailVM)
    viewModelOf(::WorldBookListVM)
    viewModelOf(::AssistantWorldBookListVM)
    viewModelOf(::WorldBookDetailVM)
    viewModelOf(::RegexGroupDetailVM)
    viewModelOf(::AssistantRegexListVM)
    viewModelOf(::RegexListVM)
    viewModelOf(::UserPersonaListVM)
    viewModelOf(::WorkspaceVM)
    viewModelOf(::WorkspaceDetailVM)
}
