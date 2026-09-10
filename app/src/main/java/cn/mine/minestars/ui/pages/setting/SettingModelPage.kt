package cn.mine.minestars.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.AiMagic
import me.rerere.hugeicons.stroke.Edit01
import me.rerere.hugeicons.stroke.FileZip
import me.rerere.hugeicons.stroke.Message01
import me.rerere.hugeicons.stroke.MessageMultiple01
import me.rerere.hugeicons.stroke.Notebook01
import me.rerere.hugeicons.stroke.Translate
import me.rerere.hugeicons.stroke.View
import me.rerere.hugeicons.stroke.Zap
import cn.mine.ai.provider.Model
import cn.mine.ai.provider.ModelType
import cn.mine.ai.provider.ProviderSetting
import cn.mine.minestars.R
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.db.entity.ModelSelectionEntity
import cn.mine.minestars.ui.components.ai.ModelSelector
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.plus
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

@Composable
fun SettingModelPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val aiSettings by vm.aiSettings.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle(emptyList())
    val modelSelections by vm.modelSelections.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = CustomColors.topBarColors.containerColor,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.setting_model_page_title)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = CustomColors.cardColorsOnSurfaceContainer.containerColor
            ) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    icon = { Icon(HugeIcons.AiMagic, null) },
                    label = { Text(stringResource(R.string.setting_model_page_tab_model)) }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    icon = { Icon(HugeIcons.Edit01, null) },
                    label = { Text(stringResource(R.string.setting_model_page_tab_prompt)) }
                )
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { contentPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> ModelSettingsPage(settings = settings, aiSettings = aiSettings, providers = providers, modelSelections = modelSelections, vm = vm, contentPadding = contentPadding)
                1 -> PromptSettingsPage(settings = settings, aiSettings = aiSettings, vm = vm, contentPadding = contentPadding)
            }
        }
    }
}

@Composable
private fun ModelSettingsPage(
    settings: Settings,
    aiSettings: cn.mine.minestars.data.datastore.AiSettings,
    providers: List<ProviderSetting>,
    modelSelections: ModelSelectionEntity,
    vm: SettingVM,
    contentPadding: PaddingValues,
) {
    fun selectUuid(value: String?) = value?.let { runCatching { Uuid.parse(it) }.getOrNull() }
    val chatModelId = selectUuid(modelSelections.chatModelId)
    val fastModelId = selectUuid(modelSelections.fastModelId)
    val titleModelId = selectUuid(modelSelections.titleModelId)
    val translateModelId = selectUuid(modelSelections.translateModelId)
    val suggestionModelId = selectUuid(modelSelections.suggestionModelId)
    val ocrModelId = selectUuid(modelSelections.ocrModelId)
    val compressModelId = selectUuid(modelSelections.compressModelId)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ModelFeatureCard(
                icon = { Icon(HugeIcons.Message01, null) },
                title = { Text(stringResource(R.string.setting_model_page_chat_model), maxLines = 1) },
                description = { Text(stringResource(R.string.setting_model_page_chat_model_desc)) },
                actions = {
                    ModelSelector(
                        modelId = chatModelId,
                        type = ModelType.CHAT,
                        onSelect = { vm.updateModelSelection(chatModelId = it.id.toString()) },
                        providers = providers,
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
        item {
            ModelFeatureCard(
                icon = { Icon(HugeIcons.Zap, null) },
                title = { Text(stringResource(R.string.setting_model_page_fast_model), maxLines = 1) },
                description = { Text(stringResource(R.string.setting_model_page_fast_model_desc)) },
                actions = {
                    ModelSelector(
                        modelId = fastModelId,
                        type = ModelType.CHAT,
                        onSelect = { vm.updateModelSelection(fastModelId = it.id.toString()) },
                        providers = providers,
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
        item {
            ModelFeatureCard(
                icon = { Icon(HugeIcons.Notebook01, null) },
                title = { Text(stringResource(R.string.setting_model_page_title_model), maxLines = 1) },
                description = { Text(stringResource(R.string.setting_model_page_title_model_desc)) },
                actions = {
                    ModelSelector(
                        modelId = titleModelId,
                        type = ModelType.CHAT,
                        onSelect = { vm.updateModelSelection(titleModelId = it.id.toString()) },
                        providers = providers,
                        allowClear = true,
                        onClear = { vm.clearTitleModel() },
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
        item {
            SuggestionModelFeatureCard(
                aiSettings = aiSettings,
                providers = providers,
                suggestionModelId = suggestionModelId,
                onSelect = { vm.updateModelSelection(suggestionModelId = it.id.toString()) },
                onClear = { vm.clearSuggestionModel() },
                onUpdateAiSettings = { vm.updateAiSettings(it) },
            )
        }
        item {
            ModelFeatureCard(
                icon = { Icon(HugeIcons.Translate, null) },
                title = { Text(stringResource(R.string.setting_model_page_translate_model), maxLines = 1) },
                description = { Text(stringResource(R.string.setting_model_page_translate_model_desc)) },
                actions = {
                    ModelSelector(
                        modelId = translateModelId,
                        type = ModelType.CHAT,
                        onSelect = { vm.updateModelSelection(translateModelId = it.id.toString()) },
                        providers = providers,
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
        item {
            ModelFeatureCard(
                icon = { Icon(HugeIcons.View, null) },
                title = { Text(stringResource(R.string.setting_model_page_ocr_model), maxLines = 1) },
                description = { Text(stringResource(R.string.setting_model_page_ocr_model_desc)) },
                actions = {
                    ModelSelector(
                        modelId = ocrModelId,
                        type = ModelType.CHAT,
                        onSelect = { vm.updateModelSelection(ocrModelId = it.id.toString()) },
                        providers = providers,
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
        item {
            ModelFeatureCard(
                icon = { Icon(HugeIcons.FileZip, null) },
                title = { Text(stringResource(R.string.setting_model_page_compress_model), maxLines = 1) },
                description = { Text(stringResource(R.string.setting_model_page_compress_model_desc)) },
                actions = {
                    ModelSelector(
                        modelId = compressModelId,
                        type = ModelType.CHAT,
                        onSelect = { vm.updateModelSelection(compressModelId = it.id.toString()) },
                        providers = providers,
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
    }
}

@Composable
private fun ModelFeatureCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CustomColors.listItemColors.containerColor,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                        title()
                    }
                    ProvideTextStyle(
                        MaterialTheme.typography.bodySmall.copy(
                            color = LocalContentColor.current.copy(alpha = 0.6f),
                        )
                    ) {
                        description()
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }
        }
    }
}

@Composable
private fun SuggestionModelFeatureCard(
    aiSettings: cn.mine.minestars.data.datastore.AiSettings,
    providers: List<ProviderSetting>,
    suggestionModelId: Uuid?,
    onSelect: (Model) -> Unit,
    onClear: () -> Unit,
    onUpdateAiSettings: (cn.mine.minestars.data.datastore.AiSettings) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CustomColors.listItemColors.containerColor,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(HugeIcons.MessageMultiple01, null)
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                        Text(stringResource(R.string.setting_model_page_suggestion_model), maxLines = 1)
                    }
                    ProvideTextStyle(
                        MaterialTheme.typography.bodySmall.copy(
                            color = LocalContentColor.current.copy(alpha = 0.6f),
                        )
                    ) {
                        Text(stringResource(R.string.setting_model_page_suggestion_model_desc))
                    }
                }
                Switch(
                    checked = aiSettings.enableSuggestion,
                    onCheckedChange = { onUpdateAiSettings(aiSettings.copy(enableSuggestion = it)) },
                )
            }

            if (aiSettings.enableSuggestion) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ModelSelector(
                        modelId = suggestionModelId,
                        type = ModelType.CHAT,
                        onSelect = onSelect,
                        providers = providers,
                        allowClear = true,
                        onClear = onClear,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
