package cn.mine.minestars.di

import cn.mine.minestars.core.workspace.ProotShellRunner
import cn.mine.minestars.core.workspace.RootfsInstaller
import cn.mine.minestars.core.workspace.RootfsPatcher
import cn.mine.minestars.core.workspace.WorkspaceConfig
import cn.mine.minestars.core.workspace.WorkspaceManager
import cn.mine.minestars.data.repository.WorkspaceRepository
import org.koin.dsl.module
import android.content.Context
import java.io.File

val workspaceModule = module {
    single {
        val context: Context = get()
        val baseDir = File(context.filesDir, "workspaces")
        WorkspaceManager(
            baseDir = baseDir,
            config = WorkspaceConfig(),
            prootShellRunner = get(),
        )
    }

    single {
        val context: Context = get()
        ProotShellRunner(
            nativeLibraryDir = File(context.applicationInfo.nativeLibraryDir),
        )
    }

    single {
        RootfsInstaller(
            manager = get(),
            patcher = RootfsPatcher(),
        )
    }

    single {
        WorkspaceRepository(
            workspaceDao = get(),
            workspaceManager = get(),
            rootfsInstaller = get(),
            assistantDao = get(),
        )
    }
}
