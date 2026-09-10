package cn.mine.minestars.ui.pages.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.ai.provider.ProviderSetting
import cn.mine.minestars.data.datastore.AiSettings
import cn.mine.minestars.data.datastore.AiSettingsStore
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.ai.mcp.McpManager
import cn.mine.minestars.data.db.toProviderSettings
import cn.mine.minestars.data.db.dao.ModelSelectionDAO
import cn.mine.minestars.data.db.dao.ProviderDAO
import cn.mine.minestars.data.db.entity.ModelSelectionEntity

class SettingVM(
    private val settingsStore: SettingsStore,
    private val aiSettingsStore: AiSettingsStore,
    private val mcpManager: McpManager,
    private val providerDAO: ProviderDAO,
    private val modelSelectionDAO: ModelSelectionDAO,
) :
    ViewModel() {
    val settings: StateFlow<Settings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings.dummy())

    val aiSettings: StateFlow<AiSettings> = aiSettingsStore.flow
        .stateIn(viewModelScope, SharingStarted.Lazily, AiSettings.dummy())

    val providers: StateFlow<List<ProviderSetting>> = providerDAO.getAllFlow()
        .map { it.toProviderSettings() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val modelSelections: StateFlow<ModelSelectionEntity> = modelSelectionDAO.getFlow()
        .map { it ?: ModelSelectionEntity() }
        .stateIn(viewModelScope, SharingStarted.Lazily, ModelSelectionEntity())

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsStore.update(settings)
        }
    }

    fun updateAiSettings(aiSettings: AiSettings) {
        viewModelScope.launch {
            aiSettingsStore.update(aiSettings)
        }
    }

    fun clearTitleModel() {
        viewModelScope.launch {
            val current = modelSelectionDAO.get() ?: ModelSelectionEntity()
            modelSelectionDAO.set(current.copy(titleModelId = null))
        }
    }

    fun clearSuggestionModel() {
        viewModelScope.launch {
            val current = modelSelectionDAO.get() ?: ModelSelectionEntity()
            modelSelectionDAO.set(current.copy(suggestionModelId = null))
        }
    }

    fun updateModelSelection(
        chatModelId: String? = null,
        fastModelId: String? = null,
        titleModelId: String? = null,
        imageGenerationModelId: String? = null,
        translateModelId: String? = null,
        suggestionModelId: String? = null,
        ocrModelId: String? = null,
        compressModelId: String? = null,
    ) {
        viewModelScope.launch {
            val current = modelSelectionDAO.get() ?: ModelSelectionEntity()
            modelSelectionDAO.set(
                current.copy(
                    chatModelId = chatModelId ?: current.chatModelId,
                    fastModelId = fastModelId ?: current.fastModelId,
                    titleModelId = titleModelId ?: current.titleModelId,
                    imageGenerationModelId = imageGenerationModelId ?: current.imageGenerationModelId,
                    translateModelId = translateModelId ?: current.translateModelId,
                    suggestionModelId = suggestionModelId ?: current.suggestionModelId,
                    ocrModelId = ocrModelId ?: current.ocrModelId,
                    compressModelId = compressModelId ?: current.compressModelId,
                )
            )
        }
    }
}
