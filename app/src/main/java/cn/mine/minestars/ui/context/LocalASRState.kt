package cn.mine.minestars.ui.context

import androidx.compose.runtime.compositionLocalOf
import cn.mine.minestars.ui.hooks.CustomAsrState

val LocalASRState = compositionLocalOf<CustomAsrState> { error("Not provided yet") }

