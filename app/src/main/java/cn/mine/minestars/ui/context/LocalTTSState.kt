package cn.mine.minestars.ui.context

import androidx.compose.runtime.compositionLocalOf
import cn.mine.minestars.ui.hooks.CustomTtsState

val LocalTTSState = compositionLocalOf<CustomTtsState> { error("Not provided yet") }
