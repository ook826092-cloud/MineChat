package cn.mine.minestars.ui.pages.developer

import androidx.lifecycle.ViewModel
import cn.mine.minestars.data.ai.AILoggingManager

class DeveloperVM(
    private val aiLoggingManager: AILoggingManager
) : ViewModel() {
    val logs = aiLoggingManager.getLogs()
}
