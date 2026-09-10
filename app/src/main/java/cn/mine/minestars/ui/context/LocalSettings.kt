package cn.mine.minestars.ui.context

import androidx.compose.runtime.staticCompositionLocalOf
import cn.mine.minestars.data.datastore.Settings

val LocalSettings = staticCompositionLocalOf<Settings> {
    error("No SettingsStore provided")
}
