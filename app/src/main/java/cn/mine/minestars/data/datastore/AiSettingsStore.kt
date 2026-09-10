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
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import cn.mine.minestars.AppScope
import cn.mine.minestars.data.ai.prompts.DEFAULT_COMPRESS_PROMPT
import cn.mine.minestars.data.ai.prompts.DEFAULT_OCR_PROMPT
import cn.mine.minestars.data.ai.prompts.DEFAULT_SUGGESTION_PROMPT
import cn.mine.minestars.data.ai.prompts.DEFAULT_TITLE_PROMPT
import cn.mine.minestars.data.ai.prompts.DEFAULT_TRANSLATION_PROMPT
import cn.mine.minestars.utils.JsonInstant
import cn.mine.minestars.utils.toMutableStateFlow
import cn.mine.search.SearchCommonOptions
import java.io.IOException
import kotlin.uuid.Uuid

private val Context.aiSettingsStore by preferencesDataStore(name = "ai_settings")

@Serializable
data class AiSettings(
    @kotlinx.serialization.Transient val init: Boolean = false,
    val enableWebSearch: Boolean = false,
    val favoriteModels: List<Uuid> = emptyList(),
    val titlePrompt: String = DEFAULT_TITLE_PROMPT,
    val translatePrompt: String = DEFAULT_TRANSLATION_PROMPT,
    val translateThinkingBudget: Int = 0,
    val enableSuggestion: Boolean = false,
    val suggestionPrompt: String = DEFAULT_SUGGESTION_PROMPT,
    val ocrPrompt: String = DEFAULT_OCR_PROMPT,
    val compressPrompt: String = DEFAULT_COMPRESS_PROMPT,
    val enableRag: Boolean = false,
    val searchCommonOptions: SearchCommonOptions = SearchCommonOptions(),
    val searchServiceSelected: Int = 0,
    val selectedTTSProviderId: Uuid = DEFAULT_SYSTEM_TTS_ID,
    val selectedASRProviderId: Uuid? = null,
) {
    companion object {
        fun dummy() = AiSettings(init = true)
    }
}

class AiSettingsStore(
    context: Context,
    scope: AppScope,
) {
    companion object {
        val ENABLE_WEB_SEARCH = booleanPreferencesKey("enable_web_search")
        val FAVORITE_MODELS = stringPreferencesKey("favorite_models")
        val TITLE_PROMPT = stringPreferencesKey("title_prompt")
        val TRANSLATION_PROMPT = stringPreferencesKey("translation_prompt")
        val TRANSLATE_THINKING_BUDGET = intPreferencesKey("translate_thinking_budget")
        val SUGGESTION_PROMPT = stringPreferencesKey("suggestion_prompt")
        val OCR_PROMPT = stringPreferencesKey("ocr_prompt")
        val COMPRESS_PROMPT = stringPreferencesKey("compress_prompt")
        val ENABLE_RAG = booleanPreferencesKey("enable_rag")
        val SEARCH_COMMON = stringPreferencesKey("search_common")
        val SEARCH_SELECTED = intPreferencesKey("search_selected")
        val SELECTED_TTS_PROVIDER = stringPreferencesKey("selected_tts_provider")
        val SELECTED_ASR_PROVIDER = stringPreferencesKey("selected_asr_provider")
    }

    private val dataStore = context.aiSettingsStore

    private val _flow = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.map { preferences ->
            AiSettings(
                enableWebSearch = preferences[ENABLE_WEB_SEARCH] == true,
                favoriteModels = preferences[FAVORITE_MODELS]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                titlePrompt = preferences[TITLE_PROMPT] ?: DEFAULT_TITLE_PROMPT,
                translatePrompt = preferences[TRANSLATION_PROMPT] ?: DEFAULT_TRANSLATION_PROMPT,
                translateThinkingBudget = preferences[TRANSLATE_THINKING_BUDGET] ?: 0,
                suggestionPrompt = preferences[SUGGESTION_PROMPT] ?: DEFAULT_SUGGESTION_PROMPT,
                ocrPrompt = preferences[OCR_PROMPT] ?: DEFAULT_OCR_PROMPT,
                compressPrompt = preferences[COMPRESS_PROMPT] ?: DEFAULT_COMPRESS_PROMPT,
                enableRag = preferences[ENABLE_RAG] == true,
                searchCommonOptions = JsonInstant.decodeFromString(
                    preferences[SEARCH_COMMON] ?: "{}"
                ),
                searchServiceSelected = preferences[SEARCH_SELECTED] ?: 0,
                selectedTTSProviderId = preferences[SELECTED_TTS_PROVIDER]?.let { Uuid.parse(it) }
                    ?: DEFAULT_SYSTEM_TTS_ID,
                selectedASRProviderId = preferences[SELECTED_ASR_PROVIDER]?.let { Uuid.parse(it) },
            )
        }
        .distinctUntilChanged()
        .toMutableStateFlow(scope, AiSettings.dummy())

    val flow = _flow

    suspend fun update(settings: AiSettings) {
        if (settings.init) return
        _flow.value = settings
        dataStore.edit { preferences ->
            preferences[ENABLE_WEB_SEARCH] = settings.enableWebSearch
            preferences[FAVORITE_MODELS] = JsonInstant.encodeToString(settings.favoriteModels)
            preferences[TITLE_PROMPT] = settings.titlePrompt
            preferences[TRANSLATION_PROMPT] = settings.translatePrompt
            preferences[TRANSLATE_THINKING_BUDGET] = settings.translateThinkingBudget
            preferences[SUGGESTION_PROMPT] = settings.suggestionPrompt
            preferences[OCR_PROMPT] = settings.ocrPrompt
            preferences[COMPRESS_PROMPT] = settings.compressPrompt
            preferences[ENABLE_RAG] = settings.enableRag
            preferences[SEARCH_COMMON] = JsonInstant.encodeToString(settings.searchCommonOptions)
            preferences[SEARCH_SELECTED] = settings.searchServiceSelected
            preferences[SELECTED_TTS_PROVIDER] = settings.selectedTTSProviderId.toString()
            settings.selectedASRProviderId?.let {
                preferences[SELECTED_ASR_PROVIDER] = it.toString()
            } ?: preferences.remove(SELECTED_ASR_PROVIDER)
        }
    }

    suspend fun update(fn: (AiSettings) -> AiSettings) {
        update(fn(_flow.value))
    }
}
