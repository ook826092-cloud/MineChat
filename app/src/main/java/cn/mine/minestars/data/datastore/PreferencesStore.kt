package cn.mine.minestars.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import cn.mine.minestars.AppScope
import cn.mine.minestars.data.model.Avatar
import cn.mine.minestars.ui.theme.CustomTheme
import cn.mine.minestars.ui.theme.PresetThemes
import cn.mine.minestars.data.db.dao.ModelSelectionDAO
import cn.mine.minestars.data.db.entity.ModelSelectionEntity
import cn.mine.minestars.utils.JsonInstant
import cn.mine.minestars.utils.toMutableStateFlow
import java.io.IOException
import kotlin.uuid.Uuid

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsStore(
    context: Context,
    scope: AppScope,
) {
    companion object {
        // ═══ Migration keys from DataStore → Room (v1→v2) ═══
        val MIGRATION_SELECT_MODEL = stringPreferencesKey("chat_model")
        val MIGRATION_TITLE_MODEL = stringPreferencesKey("title_model")
        val MIGRATION_TRANSLATE_MODEL = stringPreferencesKey("translate_model")
        val MIGRATION_SUGGESTION_MODEL = stringPreferencesKey("suggestion_model")
        val MIGRATION_IMAGE_GENERATION_MODEL = stringPreferencesKey("image_generation_model")
        val MIGRATION_OCR_MODEL = stringPreferencesKey("ocr_model")
        val MIGRATION_COMPRESS_MODEL = stringPreferencesKey("compress_model")

        val VERSION = intPreferencesKey("data_version")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val THEME_ID = stringPreferencesKey("theme_id")
        val CUSTOM_THEMES = stringPreferencesKey("custom_themes")
        val DISPLAY_SETTING = stringPreferencesKey("display_setting")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val RECORD_HTTP_RESPONSE_BODY = booleanPreferencesKey("record_http_response_body")
        val SELECT_ASSISTANT = stringPreferencesKey("select_assistant")
        val BACKUP_REMINDER_CONFIG = stringPreferencesKey("backup_reminder_config")
        val LAUNCH_COUNT = intPreferencesKey("launch_count")
        val SPONSOR_ALERT_DISMISSED_AT = intPreferencesKey("sponsor_alert_dismissed_at")
    }

    private val dataStore = context.settingsStore

    private val _settingsFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            Settings(
                dynamicColor = preferences[DYNAMIC_COLOR] ?: false,
                themeId = preferences[THEME_ID] ?: "black",
                customThemes = preferences[CUSTOM_THEMES]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                developerMode = preferences[DEVELOPER_MODE] == true,
                recordHttpResponseBody = preferences[RECORD_HTTP_RESPONSE_BODY] == true,
                displaySetting = JsonInstant.decodeFromString(
                    preferences[DISPLAY_SETTING] ?: "{}"
                ),
                assistantId = preferences[SELECT_ASSISTANT]?.let { Uuid.parse(it) }
                    ?: DEFAULT_ASSISTANT_ID,
                backupReminderConfig = JsonInstant.decodeFromString(
                    preferences[BACKUP_REMINDER_CONFIG] ?: "{}"
                ),
                launchCount = preferences[LAUNCH_COUNT] ?: 0,
                sponsorAlertDismissedAt = preferences[SPONSOR_ALERT_DISMISSED_AT] ?: 0,
            )
        }
        .distinctUntilChanged()
        .toMutableStateFlow(scope, Settings.dummy())

    val settingsFlow = _settingsFlow

    suspend fun update(settings: Settings) {
        if (settings.init) return
        _settingsFlow.value = settings
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = settings.dynamicColor
            preferences[THEME_ID] = settings.themeId
            preferences[CUSTOM_THEMES] = JsonInstant.encodeToString(settings.customThemes)
            preferences[DEVELOPER_MODE] = settings.developerMode
            preferences[RECORD_HTTP_RESPONSE_BODY] = settings.recordHttpResponseBody
            preferences[DISPLAY_SETTING] = JsonInstant.encodeToString(settings.displaySetting)
            preferences[SELECT_ASSISTANT] = settings.assistantId.toString()
            preferences[BACKUP_REMINDER_CONFIG] = JsonInstant.encodeToString(settings.backupReminderConfig)
            preferences[LAUNCH_COUNT] = settings.launchCount
            preferences[SPONSOR_ALERT_DISMISSED_AT] = settings.sponsorAlertDismissedAt
        }
    }

    suspend fun update(fn: (Settings) -> Settings) {
        update(fn(_settingsFlow.value))
    }

    suspend fun updateAssistant(assistantId: Uuid) {
        dataStore.edit { preferences ->
            preferences[SELECT_ASSISTANT] = assistantId.toString()
        }
    }

    /**
     * 迁移 v1→v2 时丢失的旧 DataStore 模型选择到 Room
     */
    suspend fun migrateModelSelections(modelSelectionDAO: ModelSelectionDAO) {
        val prefs = dataStore.data.first()
        val oldChatModelId = prefs[MIGRATION_SELECT_MODEL]
        val oldTitleModelId = prefs[MIGRATION_TITLE_MODEL]
        val oldTranslateModelId = prefs[MIGRATION_TRANSLATE_MODEL]
        val oldSuggestionModelId = prefs[MIGRATION_SUGGESTION_MODEL]
        val oldImageGenModelId = prefs[MIGRATION_IMAGE_GENERATION_MODEL]
        val oldOcrModelId = prefs[MIGRATION_OCR_MODEL]
        val oldCompressModelId = prefs[MIGRATION_COMPRESS_MODEL]

        val hasOldData = oldChatModelId != null || oldTitleModelId != null ||
            oldTranslateModelId != null || oldSuggestionModelId != null ||
            oldImageGenModelId != null || oldOcrModelId != null ||
            oldCompressModelId != null
        if (!hasOldData) return

        val current = modelSelectionDAO.get()
        val alreadyMigrated = current != null && (
            current.titleModelId != null || current.translateModelId != null ||
                current.suggestionModelId != null || current.imageGenerationModelId != null ||
                current.ocrModelId != null || current.compressModelId != null
            )
        if (alreadyMigrated) return

        modelSelectionDAO.set(
            ModelSelectionEntity(
                chatModelId = oldChatModelId ?: current?.chatModelId,
                titleModelId = oldTitleModelId,
                translateModelId = oldTranslateModelId,
                suggestionModelId = oldSuggestionModelId,
                imageGenerationModelId = oldImageGenModelId,
                ocrModelId = oldOcrModelId,
                compressModelId = oldCompressModelId,
            )
        )
    }
}

@Serializable
data class Settings(
    @kotlinx.serialization.Transient val init: Boolean = false,
    val dynamicColor: Boolean = false,
    val themeId: String = "black",
    val customThemes: List<CustomTheme> = emptyList(),
    val developerMode: Boolean = false,
    val recordHttpResponseBody: Boolean = false,
    val displaySetting: DisplaySetting = DisplaySetting(),
    val assistantId: Uuid = DEFAULT_ASSISTANT_ID,
    val backupReminderConfig: BackupReminderConfig = BackupReminderConfig(),
    val launchCount: Int = 0,
    val sponsorAlertDismissedAt: Int = 0,
) {
    companion object {
        fun dummy() = Settings(init = true)
    }
}

@Serializable
enum class ChatFontFamily {
    @SerialName("default") DEFAULT,
    @SerialName("serif") SERIF,
    @SerialName("monospace") MONOSPACE,
    @SerialName("custom") CUSTOM,
}

@Serializable
data class DisplaySetting(
    val userAvatar: Avatar = Avatar.Dummy,
    val userNickname: String = "",
    val showUserAvatar: Boolean = true,
    val showAssistantBubble: Boolean = false,
    val showUserBubble: Boolean = true,
    val bubbleOpacity: Float = 1.0f,
    val showModelIcon: Boolean = true,
    val showModelName: Boolean = true,
    val showDateTimeInMessage: Boolean = false,
    val showTokenUsage: Boolean = true,
    val showThinkingContent: Boolean = true,
    val autoCloseThinking: Boolean = true,
    val showUpdates: Boolean = true,
    val showMessageJumper: Boolean = true,
    val messageJumperOnLeft: Boolean = false,
    val fontSizeRatio: Float = 1.0f,
    val enableMessageGenerationHapticEffect: Boolean = false,
    val skipCropImage: Boolean = false,
    val enableNotificationOnMessageGeneration: Boolean = false,
    val enableLiveUpdateNotification: Boolean = false,
    val codeBlockAutoWrap: Boolean = false,
    val codeBlockAutoCollapse: Boolean = false,
    val showLineNumbers: Boolean = false,
    val ttsOnlyReadQuoted: Boolean = false,
    val autoPlayTTSAfterGeneration: Boolean = false,
    val pasteLongTextAsFile: Boolean = false,
    val pasteLongTextThreshold: Int = 1000,
    val sendOnEnter: Boolean = false,
    val enableAutoScroll: Boolean = true,
    val enableLatexRendering: Boolean = true,
    val enableBlurEffect: Boolean = false,
    val chatFontFamily: ChatFontFamily = ChatFontFamily.DEFAULT,
    val chatCustomFontPath: String = "",
    val chatCustomFontName: String = "",
    val enableVolumeKeyScroll: Boolean = false,
    val volumeKeyScrollRatio: Float = 1.0f,
)

@Serializable
data class BackupReminderConfig(
    val enabled: Boolean = false,
    val intervalDays: Int = 7,
    val lastBackupTime: Long = 0L,
)

internal val DEFAULT_ASSISTANT_ID = Uuid.parse("0950e2dc-9bd5-4801-afa3-aa887aa36b4e")
internal val DEFAULT_SYSTEM_TTS_ID = Uuid.parse("026a01a2-c3a0-4fd5-8075-80e03bdef200")
