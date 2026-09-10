package cn.mine.minestars.di

import cn.mine.minestars.data.files.FilesManager
import cn.mine.minestars.data.files.SkillManager
import cn.mine.minestars.data.repository.ConversationRepository
import cn.mine.minestars.data.repository.FavoriteRepository
import cn.mine.minestars.data.repository.FilesRepository
import cn.mine.minestars.data.repository.FolderRepository
import cn.mine.minestars.data.repository.GenMediaRepository
import cn.mine.minestars.data.repository.MemoryRepository
import org.koin.dsl.module

val repositoryModule = module {
    single {
        ConversationRepository(get(), get(), get(), get(), get(), get())
    }

    single {
        FolderRepository(get(), get())
    }

    single {
        MemoryRepository(get())
    }

    single {
        GenMediaRepository(get())
    }

    single {
        FilesRepository(get())
    }

    single {
        FavoriteRepository(get())
    }

    single {
        FilesManager(get(), get(), get())
    }

    single {
        SkillManager(get(), get(), get())
    }
}
