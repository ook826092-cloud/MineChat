package cn.mine.minestars.ui.hooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.toModel
import cn.mine.minestars.data.model.Assistant
import org.koin.compose.koinInject

@Composable
fun rememberAssistantState(
    settings: Settings,
    onUpdateSettings: (Settings) -> Unit
): AssistantState {
    val assistantDao: AssistantDAO = koinInject()
    val assistant by assistantDao.getByIdFlow(settings.assistantId.toString())
        .map { it?.toModel() ?: Assistant() }
        .collectAsStateWithLifecycle(Assistant())
    val state = remember(settings, onUpdateSettings) {
        AssistantState(settings, onUpdateSettings)
    }
    state.currentAssistant = assistant
    return state
}

class AssistantState(
    private val settings: Settings,
    private val onUpdateSettings: (Settings) -> Unit
) {
    var currentAssistant by mutableStateOf(Assistant())

    fun setSelectAssistant(assistant: Assistant) {
        onUpdateSettings(
            settings.copy(
                assistantId = assistant.id
            )
        )
    }
}
