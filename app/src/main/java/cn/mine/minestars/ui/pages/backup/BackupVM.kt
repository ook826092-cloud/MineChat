package cn.mine.minestars.ui.pages.backup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.dao.ProviderDAO
import cn.mine.minestars.data.db.toEntity
import cn.mine.minestars.data.db.toModel
import cn.mine.minestars.data.db.toProviderSettings
import cn.mine.minestars.data.repository.ConversationRepository
import cn.mine.minestars.data.sync.LocalBackupSync
import cn.mine.minestars.data.sync.importer.ChatboxImporter
import cn.mine.minestars.data.sync.importer.CherryStudioProviderImporter
import java.io.File

private const val TAG = "BackupVM"

class BackupVM(
    private val settingsStore: SettingsStore,
    private val localBackupSync: LocalBackupSync,
    private val conversationRepository: ConversationRepository,
    private val providerDAO: ProviderDAO,
    private val assistantDAO: AssistantDAO,
) : ViewModel() {
    val settings = settingsStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = Settings.dummy()
    )

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsStore.update(settings)
        }
    }

    suspend fun exportToFile(): File {
        val file = localBackupSync.prepareBackupFile()
        recordBackupTime()
        return file
    }

    suspend fun restoreFromLocalFile(file: File) {
        localBackupSync.restoreFromLocalFile(file)
    }

    suspend fun restoreFromChatBox(file: File): ChatboxRestoreResult {
        var importedConversations = 0
        var skippedExistingConversations = 0
        val result = ChatboxImporter.importStreaming(
            file = file,
            assistantId = settings.value.assistantId,
            providers = providerDAO.getAll().toProviderSettings(),
            onConversation = { conversation ->
                if (conversationRepository.existsConversationById(conversation.id)) {
                    skippedExistingConversations++
                } else {
                    conversationRepository.insertConversation(conversation)
                    importedConversations++
                }
            }
        )

        val targetAssistantId = settings.value.assistantId
        // Insert imported providers
        result.providers.forEach { provider ->
            providerDAO.insert(
                provider.toEntity(
                    type = provider::class.simpleName ?: "Unknown",
                    displayOrder = 0,
                    builtIn = provider.builtIn
                )
            )
        }
        // Update target assistant if needed
        if (result.hasConversationSystemPrompt) {
            val currentEntity = assistantDAO.getById(targetAssistantId.toString())
            if (currentEntity != null) {
                val updated = currentEntity.toModel().copy(allowConversationSystemPrompt = true)
                assistantDAO.insert(updated.toEntity())
            }
        }

        Log.i(
            TAG,
            "restoreFromChatBox: import ${result.providers.size} providers, " +
                "$importedConversations conversations, skip $skippedExistingConversations existing, " +
                "drop ${result.skippedImageParts} images"
        )
        return ChatboxRestoreResult(
            importedProviders = result.providers.size,
            importedConversations = importedConversations,
            skippedExistingConversations = skippedExistingConversations,
            skippedImageParts = result.skippedImageParts,
            skippedEmptyMessages = result.skippedEmptyMessages,
        )
    }

    fun restoreFromCherryStudio(file: File) {
        val importProviders = CherryStudioProviderImporter.importProviders(file)

        if (importProviders.isEmpty()) {
            throw IllegalArgumentException("No importable providers found in Cherry Studio backup")
        }

        Log.i(TAG, "restoreFromCherryStudio: import ${importProviders.size} providers: $importProviders")

        viewModelScope.launch {
            importProviders.forEach { provider ->
                providerDAO.insert(
                    provider.toEntity(
                        type = provider::class.simpleName ?: "Unknown",
                        displayOrder = 0,
                        builtIn = false
                    )
                )
            }
        }
    }

    private suspend fun recordBackupTime() {
        settingsStore.update { settings ->
            settings.copy(
                backupReminderConfig = settings.backupReminderConfig.copy(
                    lastBackupTime = System.currentTimeMillis()
                )
            )
        }
    }
}

data class ChatboxRestoreResult(
    val importedProviders: Int,
    val importedConversations: Int,
    val skippedExistingConversations: Int,
    val skippedImageParts: Int,
    val skippedEmptyMessages: Int,
)
