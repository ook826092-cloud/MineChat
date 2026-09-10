package cn.mine.minestars

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import kotlinx.serialization.Serializable
import cn.mine.highlight.CodeHighlighter
import cn.mine.highlight.LocalCodeHighlighter
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.db.DatabaseMigrationTracker
import cn.mine.minestars.data.db.MigrationState
import cn.mine.minestars.data.event.AppEvent
import cn.mine.minestars.data.event.AppEventBus
import cn.mine.minestars.ui.activity.SafeModeActivity
import cn.mine.minestars.ui.components.ui.TTSController
import cn.mine.minestars.ui.context.LocalASRState
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.context.LocalSettings
import cn.mine.minestars.ui.context.LocalSharedTransitionScope
import cn.mine.minestars.ui.context.LocalTTSState
import cn.mine.minestars.ui.context.LocalToaster
import cn.mine.minestars.ui.context.Navigator
import cn.mine.minestars.ui.hooks.readBooleanPreference
import cn.mine.minestars.ui.hooks.readStringPreference
import cn.mine.minestars.ui.hooks.rememberCustomAsrState
import cn.mine.minestars.ui.hooks.rememberCustomTtsState
import cn.mine.minestars.ui.pages.assistant.AssistantPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantBasicPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantDetailPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantLocalToolPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantWorkspacePage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantMcpPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantTavernBindingsPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantMemoryPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantCardExtensionsPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantCardSystemPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantAlternateGreetingsPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantCardMetaPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantPromptPage
import cn.mine.minestars.ui.pages.assistant.detail.AssistantRequestPage
import cn.mine.minestars.ui.pages.backup.BackupPage
import cn.mine.minestars.ui.pages.chat.ChatPage
import cn.mine.minestars.ui.pages.debug.DebugPage
import cn.mine.minestars.ui.pages.developer.DeveloperPage
import cn.mine.minestars.ui.pages.extensions.ExtensionsPage
import cn.mine.minestars.ui.pages.extensions.QuickMessagesPage
import cn.mine.minestars.ui.pages.extensions.SkillDetailPage
import cn.mine.minestars.ui.pages.extensions.SkillsPage
import cn.mine.minestars.ui.pages.favorite.FavoritePage
import cn.mine.minestars.ui.pages.history.HistoryPage
import cn.mine.minestars.ui.pages.imggen.ImageGenPage
import cn.mine.minestars.ui.pages.log.LogPage
import cn.mine.minestars.feature.character.CharacterCardListPage
import cn.mine.minestars.feature.tavern.PresetConfigPage
import cn.mine.minestars.feature.tavern.PresetDetailPage
import cn.mine.minestars.feature.tavern.PresetEntryDetailPage
import cn.mine.minestars.feature.tavern.PresetListPage
import cn.mine.minestars.feature.tavern.PresetParamsPage
import cn.mine.minestars.feature.tavern.PresetPromptTemplatePage
import cn.mine.minestars.feature.tavern.PresetRegexScriptDetailPage
import cn.mine.minestars.feature.tavern.PresetRegexScriptListPage
import cn.mine.minestars.feature.tavern.RegexGroupDetailPage
import cn.mine.minestars.feature.tavern.AssistantRegexScriptDetailPage
import cn.mine.minestars.feature.tavern.AssistantRegexScriptListPage
import cn.mine.minestars.feature.tavern.AssistantWorldBookListPage
import cn.mine.minestars.feature.tavern.RegexListPage
import cn.mine.minestars.feature.tavern.RegexScriptDetailPage
import cn.mine.minestars.feature.tavern.WorldBookDetailPage
import cn.mine.minestars.feature.tavern.WorldBookEntryDetailPage
import cn.mine.minestars.feature.tavern.WorldBookListPage
import cn.mine.minestars.ui.pages.search.SearchPage
import cn.mine.minestars.ui.pages.setting.SettingAboutPage
import cn.mine.minestars.ui.pages.setting.UserPersonaListPage
import cn.mine.minestars.ui.pages.setting.UserPersonaDetailPage
import cn.mine.minestars.ui.pages.setting.SettingPreferencesPage
import cn.mine.minestars.ui.pages.setting.SettingPreferencesThemePage
import cn.mine.minestars.ui.pages.setting.SettingPreferencesNotificationPage
import cn.mine.minestars.ui.pages.setting.SettingPreferencesGeneralPage
import cn.mine.minestars.ui.pages.setting.SettingPreferencesUIPage
import cn.mine.minestars.ui.pages.setting.SettingThemePage
import cn.mine.minestars.ui.pages.setting.SettingFilesPage
import cn.mine.minestars.ui.pages.setting.SettingMcpPage
import cn.mine.minestars.ui.pages.setting.SettingModelPage
import cn.mine.minestars.ui.pages.setting.SettingPage
import cn.mine.minestars.ui.pages.setting.SettingProviderDetailPage
import cn.mine.minestars.ui.pages.setting.SettingProviderPage
import cn.mine.minestars.ui.pages.setting.SettingSearchDetailPage
import cn.mine.minestars.ui.pages.setting.SettingSearchPage
import cn.mine.minestars.ui.pages.setting.SettingSpeechPage
import cn.mine.minestars.ui.pages.setting.SettingKnowledgeBasePage
import cn.mine.minestars.ui.pages.setting.SettingKnowledgeBaseDetailPage
import cn.mine.minestars.ui.pages.share.handler.ShareHandlerPage
import cn.mine.minestars.ui.pages.stats.StatsPage
import cn.mine.minestars.ui.pages.translator.TranslatorPage
import cn.mine.minestars.ui.pages.webview.WebViewPage
import cn.mine.minestars.ui.theme.LocalDarkMode
import cn.mine.minestars.ui.theme.MineChatTheme
import cn.mine.minestars.utils.CrashHandler
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

private const val TAG = "RouteActivity"

class RouteActivity : ComponentActivity() {
    private val codeHighlighter by inject<CodeHighlighter>()
    private val okHttpClient by inject<OkHttpClient>()
    private val settingsStore by inject<SettingsStore>()
    private var navStack: MutableList<NavKey>? = null

    // Volume key listener registry — last registered handler wins
    internal val volumeKeyListeners = mutableListOf<(isVolumeUp: Boolean) -> Boolean>()

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isVolumeUp = when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> true
                KeyEvent.KEYCODE_VOLUME_DOWN -> false
                else -> return super.dispatchKeyEvent(event)
            }
            if (volumeKeyListeners.lastOrNull()?.invoke(isVolumeUp) == true) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        disableNavigationBarContrast()
        super.onCreate(savedInstanceState)
        if (CrashHandler.hasCrashed(this)) {
            startActivity(Intent(this, SafeModeActivity::class.java))
            finish()
            return
        }
        setContent {
            MineChatTheme {
                @OptIn(coil3.annotation.ExperimentalCoilApi::class)
                setSingletonImageLoaderFactory { context ->
                    ImageLoader.Builder(context)
                        .crossfade(true)
                        .components {
                            add(OkHttpNetworkFetcherFactory(
                                callFactory = { okHttpClient },
                                cacheStrategy = { CacheControlCacheStrategy() },
                            ))
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                add(AnimatedImageDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                            add(SvgDecoder.Factory(scaleToDensity = true))
                        }
                        .build()
                }
                AppRoutes()
            }
        }
    }

    private fun disableNavigationBarContrast() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    @Composable
    private fun ShareHandler(backStack: MutableList<NavKey>) {
        val shareIntent = remember {
            Intent().apply {
                action = intent?.action
                putExtra(Intent.EXTRA_TEXT, intent?.getStringExtra(Intent.EXTRA_TEXT))
                putExtra(Intent.EXTRA_STREAM, intent?.getStringExtra(Intent.EXTRA_STREAM))
                putExtra(Intent.EXTRA_PROCESS_TEXT, intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT))
            }
        }

        LaunchedEffect(backStack) {
            when (shareIntent.action) {
                Intent.ACTION_SEND -> {
                    val text = shareIntent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    val imageUri = shareIntent.getStringExtra(Intent.EXTRA_STREAM)
                    backStack.add(Screen.ShareHandler(text, imageUri))
                }

                Intent.ACTION_PROCESS_TEXT -> {
                    val text = shareIntent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
                    backStack.add(Screen.ShareHandler(text, null))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Navigate to the chat screen if a conversation ID is provided
        intent.getStringExtra("conversationId")?.let { text ->
            navStack?.add(Screen.Chat(text))
        }    }

    @Composable
    fun AppRoutes() {
        val toastState = rememberToasterState()
        val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
        val tts = rememberCustomTtsState()
        val asr = rememberCustomAsrState()
        val eventBus = koinInject<AppEventBus>()
        LaunchedEffect(tts) {
            eventBus.events.collect { event ->
                when (event) {
                    is AppEvent.Speak -> tts.speak(event.text)
                }
            }
        }
        val migrationState by DatabaseMigrationTracker.state.collectAsStateWithLifecycle()

        val startScreen = Screen.Chat(
            id = if (readBooleanPreference("create_new_conversation_on_start", true)) {
                Uuid.random().toString()
            } else {
                readStringPreference(
                    "lastConversationId",
                    Uuid.random().toString()
                ) ?: Uuid.random().toString()
            }
        )

        val backStack = rememberNavBackStack(startScreen)
        SideEffect { this@RouteActivity.navStack = backStack }

        ShareHandler(backStack)

        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalNavController provides Navigator(backStack),
                LocalSharedTransitionScope provides this,
                LocalSettings provides settings,
                LocalCodeHighlighter provides codeHighlighter,
                LocalToaster provides toastState,
                LocalTTSState provides tts,
                LocalASRState provides asr,
            ) {
                Toaster(
                    state = toastState,
                    darkTheme = LocalDarkMode.current,
                    richColors = true,
                    alignment = Alignment.TopCenter,
                    showCloseButton = true,
                )
                TTSController()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    NavDisplay(
                        backStack = backStack,
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        modifier = Modifier.fillMaxSize(),
                        onBack = { backStack.removeLastOrNull() },
                        transitionSpec = {
                            if (backStack.size == 1) fadeIn() togetherWith fadeOut()
                            else {
                                slideInHorizontally { it } togetherWith
                                    slideOutHorizontally { -it / 2 } + scaleOut(targetScale = 0.7f) + fadeOut()
                            }
                        },
                        popTransitionSpec = {
                            slideInHorizontally { -it / 2 } + scaleIn(initialScale = 0.7f) + fadeIn() togetherWith
                                slideOutHorizontally { it }
                        },
                        predictivePopTransitionSpec = {
                            slideInHorizontally { -it / 2 } + scaleIn(initialScale = 0.7f) + fadeIn() togetherWith
                                slideOutHorizontally { it }
                        },
                        entryProvider = entryProvider {
                            entry<Screen.Chat>(
                                metadata = NavDisplay.transitionSpec { fadeIn() togetherWith fadeOut() }
                                        + NavDisplay.popTransitionSpec { fadeIn() togetherWith fadeOut() }
                            ) { key ->
                                ChatPage(
                                    id = Uuid.parse(key.id),
                                    text = key.text,
                                    files = key.files.map { it.toUri() },
                                    nodeId = key.nodeId?.let { Uuid.parse(it) }
                                )
                            }

                            entry<Screen.ShareHandler> { key ->
                                ShareHandlerPage(
                                    text = key.text,
                                    image = key.streamUri
                                )
                            }

                            entry<Screen.History> {
                                HistoryPage()
                            }

                            entry<Screen.Favorite> {
                                FavoritePage()
                            }

                            entry<Screen.Assistant> {
                                AssistantPage()
                            }

                            entry<Screen.AssistantDetail> { key ->
                                AssistantDetailPage(key.id)
                            }

                            entry<Screen.AssistantBasic> { key ->
                                AssistantBasicPage(key.id)
                            }

                            entry<Screen.AssistantPrompt> { key ->
                                AssistantPromptPage(key.id)
                            }

                            entry<Screen.AssistantMemory> { key ->
                                AssistantMemoryPage(key.id)
                            }

                            entry<Screen.AssistantRequest> { key ->
                                AssistantRequestPage(key.id)
                            }

                            entry<Screen.AssistantMcp> { key ->
                                AssistantMcpPage(key.id)
                            }

                            entry<Screen.AssistantLocalTool> { key ->
                                AssistantLocalToolPage(key.id)
                            }

                            entry<Screen.AssistantWorkspace> { key ->
                                AssistantWorkspacePage(key.id)
                            }

                            entry<Screen.AssistantTavernBindings> { key ->
                                AssistantTavernBindingsPage(key.id)
                            }

                            entry<Screen.AssistantCardExtensions> { key ->
                                AssistantCardExtensionsPage(key.id)
                            }

                            entry<Screen.AssistantCardSystem> { key ->
                                AssistantCardSystemPage(key.id)
                            }

                            entry<Screen.AssistantCardMeta> { key ->
                                AssistantCardMetaPage(key.id)
                            }

                            entry<Screen.AssistantAlternateGreetings> { key ->
                                AssistantAlternateGreetingsPage(key.id)
                            }

                            entry<Screen.Translator> {
                                TranslatorPage()
                            }

                            entry<Screen.Setting> {
                                SettingPage()
                            }

                            entry<Screen.Backup> {
                                BackupPage()
                            }

                            entry<Screen.ImageGen> {
                                ImageGenPage()
                            }

                            entry<Screen.WebView> { key ->
                                WebViewPage(key.url, key.content)
                            }

                            entry<Screen.SettingTheme> {
                                SettingThemePage()
                            }

                            entry<Screen.SettingPreferences> {
                                SettingPreferencesPage()
                            }

                            entry<Screen.SettingPreferencesTheme> {
                                SettingPreferencesThemePage()
                            }

                            entry<Screen.SettingPreferencesNotification> {
                                SettingPreferencesNotificationPage()
                            }

                            entry<Screen.SettingPreferencesGeneral> {
                                SettingPreferencesGeneralPage()
                            }

                            entry<Screen.SettingPreferencesUI> {
                                SettingPreferencesUIPage()
                            }

                            entry<Screen.SettingProvider> {
                                SettingProviderPage()
                            }

                            entry<Screen.SettingProviderDetail> { key ->
                                val id = Uuid.parse(key.providerId)
                                SettingProviderDetailPage(id = id)
                            }

                            entry<Screen.SettingModels> {
                                SettingModelPage()
                            }

                            entry<Screen.SettingAbout> {
                                SettingAboutPage()
                            }

                            entry<Screen.SettingUserPersona> {
                                UserPersonaListPage()
                            }

                            entry<Screen.UserPersonaDetail> { key ->
                                UserPersonaDetailPage(personaId = key.id)
                            }

                            entry<Screen.SettingSearch> {
                                SettingSearchPage()
                            }

                            entry<Screen.SettingSearchDetail> { key ->
                                val id = Uuid.parse(key.serviceId)
                                SettingSearchDetailPage(id)
                            }

                            entry<Screen.SettingSpeech> {
                                SettingSpeechPage()
                            }

                            entry<Screen.SettingMcp> {
                                SettingMcpPage()
                            }

                            entry<Screen.SettingFiles> {
                                SettingFilesPage()
                            }

                            entry<Screen.SettingKnowledgeBase> {
                                SettingKnowledgeBasePage()
                            }

                            entry<Screen.SettingKnowledgeBaseDetail> { key ->
                                SettingKnowledgeBaseDetailPage(kbId = key.id)
                            }

                            entry<Screen.Developer> {
                                DeveloperPage()
                            }

                            entry<Screen.Debug> {
                                DebugPage()
                            }

                            entry<Screen.Log> {
                                LogPage()
                            }

                            entry<Screen.Extensions> {
                                ExtensionsPage()
                            }

                            entry<Screen.QuickMessages> {
                                QuickMessagesPage()
                            }

                            entry<Screen.Skills> {
                                SkillsPage()
                            }

                            entry<Screen.SkillDetail> { key ->
                                SkillDetailPage(skillName = key.skillName)
                            }

                            entry<Screen.MessageSearch> {
                                SearchPage()
                            }

                            entry<Screen.CharacterCards> {
                                CharacterCardListPage()
                            }

                            entry<Screen.Presets> {
                                PresetListPage()
                            }

                            entry<Screen.PresetDetail> { key ->
                                PresetDetailPage(presetId = key.id)
                            }

                            entry<Screen.PresetEntryDetail> { key ->
                                PresetEntryDetailPage(presetId = key.presetId, entryIndex = key.entryIndex)
                            }

                            entry<Screen.PresetParams> { key ->
                                PresetParamsPage(presetId = key.id)
                            }

                            entry<Screen.PresetConfig> { key ->
                                PresetConfigPage(presetId = key.id)
                            }

                            entry<Screen.PresetPromptTemplate> { key ->
                                PresetPromptTemplatePage(presetId = key.id)
                            }

                            entry<Screen.PresetRegexScriptList> { key ->
                                PresetRegexScriptListPage(presetId = key.id)
                            }

                            entry<Screen.PresetRegexScriptDetail> { key ->
                                PresetRegexScriptDetailPage(presetId = key.id, scriptId = key.scriptId)
                            }

                            entry<Screen.WorldBooks> {
                                WorldBookListPage()
                            }

                            entry<Screen.WorldBookDetail> { key ->
                                WorldBookDetailPage(bookId = key.id)
                            }

                            entry<Screen.WorldBookEntryDetail> { key ->
                                WorldBookEntryDetailPage(bookId = key.bookId, entryIndex = key.entryIndex, assistantId = key.assistantId)
                            }

                            entry<Screen.RegexScripts> {
                                RegexListPage()
                            }

                            entry<Screen.RegexGroupDetail> { key ->
                                RegexGroupDetailPage(groupId = key.id)
                            }

                            entry<Screen.AssistantWorldBookList> { key ->
                                AssistantWorldBookListPage(assistantId = key.assistantId)
                            }

                            entry<Screen.AssistantWorldBookDetail> { key ->
                                WorldBookDetailPage(assistantId = key.assistantId)
                            }

                            entry<Screen.AssistantRegexScriptList> { key ->
                                AssistantRegexScriptListPage(assistantId = key.assistantId)
                            }

                            entry<Screen.AssistantRegexScriptDetail> { key ->
                                AssistantRegexScriptDetailPage(assistantId = key.assistantId, scriptId = key.scriptId)
                            }

                            entry<Screen.RegexScriptDetail> { key ->
                                RegexScriptDetailPage(scriptId = key.id)
                            }

                            entry<Screen.Stats> {
                                StatsPage()
                            }

                            entry<Screen.Workspaces> {
                                cn.mine.minestars.ui.pages.workspace.WorkspacePage()
                            }

                            entry<Screen.WorkspaceDetail> { key ->
                                cn.mine.minestars.ui.pages.workspace.WorkspaceDetailPage(id = key.id)
                            }

                            entry<Screen.WorkspaceTerminal> { key ->
                                cn.mine.minestars.ui.pages.workspace.WorkspaceTerminalPage(id = key.id)
                            }

                            entry<Screen.WorkspaceFileEditor> { key ->
                                cn.mine.minestars.ui.pages.workspace.WorkspaceFileEditorPage(
                                    id = key.id,
                                    area = cn.mine.minestars.core.workspace.WorkspaceStorageArea.valueOf(key.area),
                                    path = key.path,
                                )
                            }
                        }
                    )
                    if (BuildConfig.DEBUG) {
                        Text(
                            text = "[开发模式]",
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                    AnimatedVisibility(
                        visible = migrationState is MigrationState.Migrating,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val state = migrationState as? MigrationState.Migrating
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = stringResource(R.string.db_migrating),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (state != null) {
                                    Text(
                                        text = "v${state.from} → v${state.to}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed interface Screen : NavKey {
    @Serializable
    data class Chat(
        val id: String,
        val text: String? = null,
        val files: List<String> = emptyList(),
        val nodeId: String? = null
    ) : Screen

    @Serializable
    data class ShareHandler(val text: String, val streamUri: String? = null) : Screen

    @Serializable
    data object History : Screen

    @Serializable
    data object Favorite : Screen

    @Serializable
    data object Assistant : Screen

    @Serializable
    data class AssistantDetail(val id: String) : Screen

    @Serializable
    data class AssistantBasic(val id: String) : Screen

    @Serializable
    data class AssistantPrompt(val id: String) : Screen

    @Serializable
    data class AssistantMemory(val id: String) : Screen

    @Serializable
    data class AssistantRequest(val id: String) : Screen

    @Serializable
    data class AssistantMcp(val id: String) : Screen

    @Serializable
    data class AssistantLocalTool(val id: String) : Screen

    @Serializable
    data class AssistantWorkspace(val id: String) : Screen

    @Serializable
    data class AssistantTavernBindings(val id: String) : Screen

    @Serializable
    data class AssistantCardExtensions(val id: String) : Screen

    @Serializable
    data class AssistantCardSystem(val id: String) : Screen

    @Serializable
    data class AssistantCardMeta(val id: String) : Screen

    @Serializable
    data class AssistantAlternateGreetings(val id: String) : Screen

    @Serializable
    data object Translator : Screen

    @Serializable
    data object Setting : Screen

    @Serializable
    data object Backup : Screen

    @Serializable
    data object ImageGen : Screen

    @Serializable
    data class WebView(val url: String = "", val content: String = "") : Screen

    @Serializable
    data object SettingTheme : Screen

    @Serializable
    data object SettingPreferences : Screen

    @Serializable
    data object SettingPreferencesTheme : Screen

    @Serializable
    data object SettingPreferencesNotification : Screen

    @Serializable
    data object SettingPreferencesGeneral : Screen

    @Serializable
    data object SettingPreferencesUI : Screen

    @Serializable
    data object SettingProvider : Screen

    @Serializable
    data class SettingProviderDetail(val providerId: String) : Screen

    @Serializable
    data object SettingModels : Screen

    @Serializable
    data object SettingAbout : Screen

    @Serializable
    data object SettingUserPersona : Screen

    @Serializable
    data class UserPersonaDetail(val id: String) : Screen

    @Serializable
    data object SettingSearch : Screen

    @Serializable
    data class SettingSearchDetail(val serviceId: String) : Screen

    @Serializable
    data object SettingKnowledgeBase : Screen

    @Serializable
    data class SettingKnowledgeBaseDetail(val id: String) : Screen

    @Serializable
    data object SettingSpeech : Screen

    @Serializable
    data object SettingMcp : Screen

    @Serializable
    data object SettingFiles : Screen

    @Serializable
    data object Developer : Screen

    @Serializable
    data object Debug : Screen

    @Serializable
    data object Log : Screen

    @Serializable
    data object Extensions : Screen

    @Serializable
    data object QuickMessages : Screen

    @Serializable
    data object Skills : Screen

    @Serializable
    data class SkillDetail(val skillName: String) : Screen

    @Serializable
    data object MessageSearch : Screen

    @Serializable
    data object CharacterCards : Screen

    @Serializable
    data object Presets : Screen

    @Serializable
    data class PresetDetail(val id: String) : Screen

    @Serializable
    data class PresetEntryDetail(val presetId: String, val entryIndex: Int) : Screen

    @Serializable
    data class PresetParams(val id: String) : Screen

    @Serializable
    data class PresetConfig(val id: String) : Screen

    @Serializable
    data class PresetPromptTemplate(val id: String) : Screen

    @Serializable
    data class PresetRegexScriptList(val id: String) : Screen

    @Serializable
    data class PresetRegexScriptDetail(val id: String, val scriptId: String) : Screen

    @Serializable
    data object WorldBooks : Screen

    @Serializable
    data object RegexScripts : Screen

    @Serializable
    data class RegexGroupDetail(val id: String) : Screen

    @Serializable
    data class AssistantWorldBookList(val assistantId: String) : Screen

    @Serializable
    data class AssistantWorldBookDetail(val assistantId: String) : Screen

    @Serializable
    data class AssistantRegexScriptList(val assistantId: String) : Screen

    @Serializable
    data class AssistantRegexScriptDetail(val assistantId: String, val scriptId: String) : Screen

    @Serializable
    data class RegexScriptDetail(val id: String) : Screen

    @Serializable
    data class WorldBookDetail(val id: String) : Screen

    @Serializable
    data class WorldBookEntryDetail(val bookId: String, val entryIndex: Int, val assistantId: String? = null) : Screen

    @Serializable
    data object Stats : Screen

    @Serializable
    data object Workspaces : Screen

    @Serializable
    data class WorkspaceDetail(val id: String) : Screen

    @Serializable
    data class WorkspaceTerminal(val id: String) : Screen

    @Serializable
    data class WorkspaceFileEditor(val id: String, val area: String, val path: String) : Screen
}
