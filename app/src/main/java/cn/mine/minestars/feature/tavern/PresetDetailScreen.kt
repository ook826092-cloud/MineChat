package cn.mine.minestars.feature.tavern

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.mine.ai.core.ReasoningLevel
import cn.mine.minestars.R
import cn.mine.minestars.Screen
import cn.mine.minestars.data.db.entity.PresetEntryEntity
import cn.mine.minestars.core.tavern.preset.PresetModelParams
import cn.mine.minestars.core.tavern.regex.RegexScript
import cn.mine.minestars.ui.components.ai.ReasoningButton
import cn.mine.minestars.ui.components.ui.BottomCreateToolbar
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.insertAtCursor
import cn.mine.minestars.utils.plus
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.ArrowLeft01
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Delete01
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetDetailPage(
    presetId: String,
    vm: PresetDetailVM = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
    ),
) {
    val navController = LocalNavController.current
    val presetName by vm.presetName.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val hasUnsavedChanges by vm.hasUnsavedChanges.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val modelParams by vm.modelParams.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    BackHandler(enabled = hasUnsavedChanges) {
        scope.launch {
            vm.save()
            navController.popBackStack()
        }
    }
    var selectedTab by remember { mutableStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { 4 })

    // Load preset data on first composition
    LaunchedEffect(presetId) {
        vm.loadPreset(presetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预设详情") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            if (hasUnsavedChanges) vm.save()
                            navController.popBackStack()
                        }
                    }) {
                        Icon(HugeIcons.ArrowLeft01, contentDescription = null)
                    }
                },
                actions = {},
                colors = CustomColors.topBarColors,
            )
        },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding(),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("信息") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("模板") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; scope.launch { pagerState.animateScrollToPage(2) } },
                    text = { Text("参数") },
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; scope.launch { pagerState.animateScrollToPage(3) } },
                    text = { Text("配置") },
                )
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                when (page) {
                    0 -> InfoTab(
                        presetName = presetName,
                        onRenamePreset = { vm.renamePreset(it) },
                        entries = entries,
                        isLoading = isLoading,
                        presetId = presetId,
                        vm = vm,
                    )
                    1 -> TemplatesTab(
                        modelParams = modelParams,
                        vm = vm,
                    )
                    2 -> ParamsTab(
                        modelParams = modelParams,
                        onUpdateTemperature = { vm.updateTemperature(it) },
                        onUpdateTopP = { vm.updateTopP(it) },
                        onUpdateTopK = { vm.updateTopK(it) },
                        onUpdateMinP = { vm.updateMinP(it) },
                        onUpdateTopA = { vm.updateTopA(it) },
                        onUpdateRepetitionPenalty = { vm.updateRepetitionPenalty(it) },
                        onUpdateFrequencyPenalty = { vm.updateFrequencyPenalty(it) },
                        onUpdatePresencePenalty = { vm.updatePresencePenalty(it) },
                        onUpdateSeed = { vm.updateSeed(it) },
                        onUpdateN = { vm.updateN(it) },
                        onUpdateMaxTokens = { vm.updateMaxTokens(it) },
                        onUpdateMaxContext = { vm.updateMaxContext(it) },
                        onUpdateStop = { vm.updateStop(it) },
                        onUpdateStreamOutput = { vm.updateStreamOutput(it) },
                        onUpdateReasoningLevel = { vm.updateReasoningLevel(it) },
                    )
                    3 -> ConfigTab(
                        characterNamesBehavior = modelParams.characterNamesBehavior,
                        continuePrefill = modelParams.continuePrefill,
                        squashSystemMessages = modelParams.squashSystemMessages,
                        functionCalling = modelParams.functionCalling,
                        sendInlineMedia = modelParams.sendInlineMedia,
                        regexScripts = modelParams.regexScripts,
                        onUpdateCharacterNamesBehavior = { vm.updateCharacterNamesBehavior(it) },
                        onUpdateContinuePrefill = { vm.updateContinuePrefill(it) },
                        onUpdateSquashSystemMessages = { vm.updateSquashSystemMessages(it) },
                        onUpdateFunctionCalling = { vm.updateFunctionCalling(it) },
                        onUpdateSendInlineMedia = { vm.updateSendInlineMedia(it) },
                        onNavigateToRegexList = { navController.navigate(Screen.PresetRegexScriptList(presetId)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun EmptySectionHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "暂无条目",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private val forceToggleIdentifiers = setOf(
    "charDescription", "charPersonality", "scenario", "personaDescription",
    "dialogueExamples", "worldInfoBefore", "worldInfoAfter", "chatHistory"
)

/**
 * Wrapper that adds SwipeToDismissBox (left-swipe) around PresetEntryCard.
 * System entries (systemPrompt=true) are locked and not swipeable.
 * Only available actions (onUnmount/onDelete) appear as buttons in the swipe background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetEntryCardSwipeable(
    entry: PresetEntryEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onUnmount: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showMountButton: Boolean = false,
    onMount: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val swipeState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()

    val hasActions = !entry.systemPrompt && (onUnmount != null || onDelete != null)

    val cardContent: @Composable () -> Unit = {
        PresetEntryCard(
            entry = entry,
            onToggleEnabled = onToggleEnabled,
            showMountButton = showMountButton,
            onMount = onMount,
        )
    }

    if (hasActions) {
        SwipeToDismissBox(
            state = swipeState,
            backgroundContent = {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Close button — dismisses the swipe without action
                    IconButton(onClick = { scope.launch { swipeState.reset() } }) {
                        Icon(HugeIcons.Cancel01, null)
                    }
                    // Unmount (only for mounted non-system entries)
                    if (onUnmount != null) {
                        FilledIconButton(onClick = {
                            scope.launch {
                                onUnmount()
                                swipeState.reset()
                            }
                        }) {
                            Icon(HugeIcons.Cancel01, "取消挂载")
                        }
                    }
                    // Delete (non-system entries)
                    if (onDelete != null) {
                        FilledIconButton(
                            onClick = {
                                scope.launch {
                                    onDelete()
                                    swipeState.reset()
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Icon(HugeIcons.Delete01, "删除")
                        }
                    }
                }
            },
            enableDismissFromStartToEnd = false,
            modifier = modifier,
        ) {
            cardContent()
        }
    } else {
        Box(modifier = modifier) {
            cardContent()
        }
    }
}

@Composable
private fun PresetEntryCard(
    entry: PresetEntryEntity,
    onToggleEnabled: (Boolean) -> Unit,
    showMountButton: Boolean = false,
    onMount: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val category = entryCategory(entry)
    val showToggle = !entry.marker || entry.identifier in forceToggleIdentifiers

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(category.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = entry.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = category.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (showMountButton && onMount != null) {
                FilledIconButton(
                    onClick = onMount,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Icon(HugeIcons.Add01, "挂载")
                }
            } else if (showToggle) {
                Switch(
                    checked = entry.enabled,
                    onCheckedChange = { onToggleEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                        checkedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        checkedBorderColor = MaterialTheme.colorScheme.outline,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
    }
}

private data class EntryCategory(
    val label: String,
    val iconRes: Int,
)

private fun entryCategory(entry: PresetEntryEntity): EntryCategory {
    // Injection
    if (entry.injectionPosition == 1) return EntryCategory("注入", R.drawable.ic_entry_injection)
    // Marker
    if (entry.marker) return EntryCategory("标记", R.drawable.ic_entry_marker)
    // Global Prompt: system_prompt without forbid_overrides
    if (entry.systemPrompt && !entry.forbidOverrides) return EntryCategory("全局提示词", R.drawable.ic_entry_global_prompt)
    // Important Prompt: system_prompt with forbid_overrides
    if (entry.systemPrompt && entry.forbidOverrides) return EntryCategory("重要提示词", R.drawable.ic_entry_important_prompt)
    // Preset Prompt (default)
    return EntryCategory("预设提示词", R.drawable.ic_entry_preset_prompt)
}

// ── Info Tab ──

@Composable
private fun InfoTab(
    presetName: String,
    onRenamePreset: (String) -> Unit,
    entries: List<PresetEntryEntity>,
    isLoading: Boolean,
    presetId: String,
    vm: PresetDetailVM,
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val mountedEntries = entries.filter { it.mounted }
        vm.reorderMounted(
            mountedEntries.indexOfFirst { it.id == from.key },
            mountedEntries.indexOfFirst { it.id == to.key },
        )
    }

    val mountedEntries = entries.filter { it.mounted }
    val unmountedEntries = entries.filterNot { it.mounted }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp) + PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            // Preset name
            item("preset_name") {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = onRenamePreset,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    textStyle = MaterialTheme.typography.titleMedium,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("未命名预设") },
                )
            }

            // Mounted section
            item("mounted_header") {
                SectionLabel("已挂载")
            }

            if (mountedEntries.isEmpty() && !isLoading) {
                item("mounted_empty") { EmptySectionHint() }
            } else {
                itemsIndexed(mountedEntries, key = { _, entry -> entry.id }) { index, entry ->
                    ReorderableItem(
                        state = reorderableState,
                        key = entry.id,
                    ) { isDragging ->
                        PresetEntryCardSwipeable(
                            entry = entry,
                            onToggleEnabled = { enabled -> vm.setEntryEnabled(entry.entryIndex, enabled) },
                            onUnmount = { vm.unmountEntry(entry.entryIndex) },
                            onDelete = { vm.deleteEntry(entry.entryIndex) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .clickable { navController.navigate(Screen.PresetEntryDetail(presetId, entry.entryIndex)) }
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    },
                                    onDragStopped = {
                                        haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    },
                                )
                                .graphicsLayer {
                                    if (isDragging) {
                                        scaleX = 0.95f
                                        scaleY = 0.95f
                                    }
                                },
                        )
                    }
                }
            }

            // Unmounted section
            item("unmounted_header") {
                SectionLabel("未挂载")
            }

            if (unmountedEntries.isEmpty() && !isLoading) {
                item("unmounted_empty") { EmptySectionHint() }
            } else {
                itemsIndexed(unmountedEntries, key = { _, entry -> entry.id }) { _, entry ->
                    PresetEntryCardSwipeable(
                        entry = entry,
                        showMountButton = true,
                        onToggleEnabled = { enabled -> vm.setEntryEnabled(entry.entryIndex, enabled) },
                        onMount = { vm.mountEntry(entry.entryIndex) },
                        onDelete = { vm.deleteEntry(entry.entryIndex) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.PresetEntryDetail(presetId, entry.entryIndex)) },
                    )
                }
            }
        }

        val expanded = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

        BottomCreateToolbar(
            text = "添加条目",
            expanded = expanded,
            onClick = {
                vm.addEntry()
                scope.launch {
                    listState.scrollToItem(listState.layoutInfo.totalItemsCount)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

// ── Template Tab ──

@Composable
private fun TemplatesTab(
    modelParams: PresetModelParams,
    vm: PresetDetailVM,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp) + PaddingValues(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        // ── Format Templates section ──
        item("format_header") {
            SectionLabel("格式模板")
        }

        item("description_format") {
            FormatFieldCard(
                label = "角色设定",
                variable = "{{description}}",
                value = modelParams.descriptionFormat,
                onValueChange = { vm.updateDescriptionFormat(it) },
                defaultValue = PresetModelParams.DEFAULT_DESCRIPTION_FORMAT,
            )
        }

        item("persona_format") {
            FormatFieldCard(
                label = "用户身份",
                variable = "{{persona}}",
                value = modelParams.personaFormat,
                onValueChange = { vm.updatePersonaFormat(it) },
                defaultValue = PresetModelParams.DEFAULT_PERSONA_FORMAT,
            )
        }

        item("personality_format") {
            FormatFieldCard(
                label = "性格",
                variable = "{{personality}}",
                value = modelParams.personalityFormat,
                onValueChange = { vm.updatePersonalityFormat(it) },
                defaultValue = PresetModelParams.DEFAULT_PERSONALITY_FORMAT,
            )
        }

        item("scenario_format") {
            FormatFieldCard(
                label = "场景",
                variable = "{{scenario}}",
                value = modelParams.scenarioFormat,
                onValueChange = { vm.updateScenarioFormat(it) },
                defaultValue = PresetModelParams.DEFAULT_SCENARIO_FORMAT,
            )
        }

        item("wi_format") {
            FormatFieldCard(
                label = "世界信息",
                variable = "{0}",
                value = modelParams.wiFormat,
                onValueChange = { vm.updateWiFormat(it) },
                defaultValue = PresetModelParams.DEFAULT_WI_FORMAT,
            )
        }

        // ── Utility Prompts section ──
        item("utility_header") {
            SectionLabel("实用提示词")
        }

        item("impersonation_prompt") {
            PromptFieldCard(
                label = "扮演提示",
                value = modelParams.impersonationPrompt,
                onValueChange = { vm.updateImpersonationPrompt(it) },
                defaultValue = PresetModelParams.DEFAULT_IMPERSONATION_PROMPT,
                placeholder = "编写 {{user}} 的下一句回复...",
            )
        }

        item("group_nudge_prompt") {
            PromptFieldCard(
                label = "群聊角色提示",
                value = modelParams.groupNudgePrompt,
                onValueChange = { vm.updateGroupNudgePrompt(it) },
                defaultValue = PresetModelParams.DEFAULT_GROUP_NUDGE_PROMPT,
                placeholder = "仅以 {{char}} 的身份回复...",
            )
        }

        item("new_chat_prompt") {
            PromptFieldCard(
                label = "新对话提示",
                value = modelParams.newChatPrompt,
                onValueChange = { vm.updateNewChatPrompt(it) },
                defaultValue = PresetModelParams.DEFAULT_NEW_CHAT_PROMPT,
                placeholder = "开始新对话时的系统提示",
            )
        }

        item("new_group_chat_prompt") {
            PromptFieldCard(
                label = "新群聊提示",
                value = modelParams.newGroupChatPrompt,
                onValueChange = { vm.updateNewGroupChatPrompt(it) },
                defaultValue = PresetModelParams.DEFAULT_NEW_GROUP_CHAT_PROMPT,
                placeholder = "开始新群聊时的系统提示",
            )
        }

        item("new_example_chat_prompt") {
            PromptFieldCard(
                label = "新示例对话提示",
                value = modelParams.newExampleChatPrompt,
                onValueChange = { vm.updateNewExampleChatPrompt(it) },
                defaultValue = PresetModelParams.DEFAULT_NEW_EXAMPLE_CHAT_PROMPT,
                placeholder = "示例对话前的系统提示",
            )
        }

        item("continue_nudge_prompt") {
            PromptFieldCard(
                label = "继续提示",
                value = modelParams.continueNudgePrompt,
                onValueChange = { vm.updateContinueNudgePrompt(it) },
                defaultValue = PresetModelParams.DEFAULT_CONTINUE_NUDGE_PROMPT,
                placeholder = "继续生成时的提示内容",
            )
        }

        item("send_if_empty") {
            PromptFieldCard(
                label = "空消息替换",
                value = modelParams.sendIfEmptyPrompt,
                onValueChange = { vm.updateSendIfEmptyPrompt(it) },
                defaultValue = PresetModelParams.DEFAULT_SEND_IF_EMPTY,
                placeholder = "用户发送空消息时使用的替代内容（留空则忽略）",
            )
        }
    }
}

// ── Params Tab ──

@Composable
private fun ParamsTab(
    modelParams: PresetModelParams,
    onUpdateTemperature: (Float?) -> Unit,
    onUpdateTopP: (Float?) -> Unit,
    onUpdateTopK: (Float?) -> Unit,
    onUpdateMinP: (Float?) -> Unit,
    onUpdateTopA: (Float?) -> Unit,
    onUpdateRepetitionPenalty: (Float?) -> Unit,
    onUpdateFrequencyPenalty: (Float?) -> Unit,
    onUpdatePresencePenalty: (Float?) -> Unit,
    onUpdateSeed: (Long?) -> Unit,
    onUpdateN: (Int?) -> Unit,
    onUpdateMaxTokens: (Int?) -> Unit,
    onUpdateMaxContext: (Int?) -> Unit,
    onUpdateStop: (List<String>) -> Unit,
    onUpdateStreamOutput: (Boolean) -> Unit,
    onUpdateReasoningLevel: (ReasoningLevel) -> Unit,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp) + PaddingValues(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        // ── Card 1: LLM 标准参数 ──
        item("llm_standard") {
            ParamCard("LLM 标准参数") {
                NullableFloatField("温度", modelParams.temperature, onUpdateTemperature, 0f..2f, 1.0f, "0 - 2")
                CompactDivider()
                NullableFloatField("Top P", modelParams.topP, onUpdateTopP, 0f..1f, 1.0f, "0 - 1")
                CompactDivider()
                NullableFloatField("频率惩罚", modelParams.frequencyPenalty, onUpdateFrequencyPenalty, -2f..2f, 0f, "-2 - 2")
                CompactDivider()
                NullableFloatField("存在惩罚", modelParams.presencePenalty, onUpdatePresencePenalty, -2f..2f, 0f, "-2 - 2")
                CompactDivider()
                NullableLongField("随机种子", modelParams.seed, onUpdateSeed, "-1 = 随机")
                CompactDivider()
                NullableIntField("生成条数", modelParams.n, onUpdateN, "每次生成条数")
                CompactDivider()
                StopEditor(value = modelParams.stop, onUpdate = onUpdateStop)
            }
        }

        // ── Card 2: 上下文与输出预算 ──
        item("context_budget") {
            ParamCard("上下文与输出预算") {
                NullableIntField("最大上下文 Token", modelParams.maxContext, onUpdateMaxContext, "空 = 不截断")
                CompactDivider()
                NullableIntField("最大输出 Token", modelParams.maxTokens, onUpdateMaxTokens, "空 = 无限制")
                CompactDivider()
                FormRowCompact("流式输出") {
                    Switch(checked = modelParams.streamOutput, onCheckedChange = onUpdateStreamOutput)
                }
                CompactDivider()
                FormRowCompact("推理预算") {
                    val reasoningLevel = try {
                        ReasoningLevel.valueOf(modelParams.reasoningLevel.uppercase())
                    } catch (_: Exception) {
                        ReasoningLevel.AUTO
                    }
                    ReasoningButton(reasoningLevel = reasoningLevel, onUpdateReasoningLevel = onUpdateReasoningLevel)
                }
            }
        }

        // ── Card 3: 扩展采样器（非 OpenAI 标准，源自生图采样）──
        item("extended_samplers") {
            ParamCard("扩展采样器") {
                NullableFloatField("Top K", modelParams.topK, onUpdateTopK, 0f..500f, 0f, "0 - 500")
                CompactDivider()
                NullableFloatField("Min P", modelParams.minP, onUpdateMinP, 0f..1f, 0f, "0 - 1")
                CompactDivider()
                NullableFloatField("Top A", modelParams.topA, onUpdateTopA, 0f..1f, 0f, "0 - 1")
                CompactDivider()
                NullableFloatField("重复惩罚", modelParams.repetitionPenalty, onUpdateRepetitionPenalty, 1f..2f, 1.0f, "1 - 2")
            }
        }
    }
}

// ── Reusable nullable numeric editors ──

@Composable
private fun NullableFloatField(
    label: String,
    value: Float?,
    onUpdate: (Float?) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultEnabled: Float,
    hint: String,
) {
    var enabled by remember(value) { mutableStateOf(value != null) }
    var input by remember(value) { mutableStateOf(value?.toString() ?: "") }

    FormRow(
        label = label,
        tail = {
            Switch(
                checked = enabled,
                onCheckedChange = { e ->
                    enabled = e
                    if (!e) {
                        onUpdate(null)
                    } else {
                        onUpdate(defaultEnabled)
                        input = defaultEnabled.toString()
                    }
                },
            )
        },
    ) {
        if (enabled) {
            val floatValue = input.toFloatOrNull()
            OutlinedTextField(
                value = input,
                onValueChange = { v ->
                    input = v
                    v.toFloatOrNull()?.takeIf { it in valueRange }?.let { onUpdate(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = floatValue == null || floatValue !in valueRange,
                supportingText = { Text(hint) },
            )
        }
    }
}

@Composable
private fun NullableLongField(
    label: String,
    value: Long?,
    onUpdate: (Long?) -> Unit,
    hint: String,
) {
    var enabled by remember(value) { mutableStateOf(value != null) }
    var input by remember(value) { mutableStateOf(value?.toString() ?: "") }

    FormRow(
        label = label,
        tail = {
            Switch(
                checked = enabled,
                onCheckedChange = { e ->
                    enabled = e
                    if (!e) {
                        onUpdate(null)
                    } else {
                        onUpdate(-1L)
                        input = "-1"
                    }
                },
            )
        },
    ) {
        if (enabled) {
            OutlinedTextField(
                value = input,
                onValueChange = { v ->
                    input = v
                    v.toLongOrNull()?.let { onUpdate(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text(hint) },
            )
        }
    }
}

@Composable
private fun NullableIntField(
    label: String,
    value: Int?,
    onUpdate: (Int?) -> Unit,
    hint: String,
) {
    var enabled by remember(value) { mutableStateOf(value != null) }
    var input by remember(value) { mutableStateOf(value?.toString() ?: "") }

    FormRow(
        label = label,
        tail = {
            Switch(
                checked = enabled,
                onCheckedChange = { e ->
                    enabled = e
                    if (!e) {
                        onUpdate(null)
                    } else {
                        onUpdate(1)
                        input = "1"
                    }
                },
            )
        },
    ) {
        if (enabled) {
            OutlinedTextField(
                value = input,
                onValueChange = { v ->
                    input = v
                    v.toIntOrNull()?.let { onUpdate(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text(hint) },
            )
        }
    }
}

@Composable
private fun StopEditor(
    value: List<String>,
    onUpdate: (List<String>) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.joinToString(", ")) }

    FormRow(label = "停止序列") {
        OutlinedTextField(
            value = text,
            onValueChange = { v ->
                text = v
                onUpdate(v.split(",").map { it.trim() }.filter { it.isNotBlank() })
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("逗号分隔的停止序列") },
        )
    }
}

// ── Config Tab ──

@Composable
private fun ConfigTab(
    characterNamesBehavior: Int,
    continuePrefill: Boolean,
    squashSystemMessages: Boolean,
    functionCalling: Boolean,
    sendInlineMedia: Boolean,
    regexScripts: List<RegexScript> = emptyList(),
    onUpdateCharacterNamesBehavior: (Int) -> Unit,
    onUpdateContinuePrefill: (Boolean) -> Unit,
    onUpdateSquashSystemMessages: (Boolean) -> Unit,
    onUpdateFunctionCalling: (Boolean) -> Unit,
    onUpdateSendInlineMedia: (Boolean) -> Unit,
    onNavigateToRegexList: () -> Unit = {},
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp) + PaddingValues(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        item("naming_behavior") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle("命名行为控制")

                    val behaviorOptions = listOf(
                        -1 to "无",
                        0 to "默认",
                        1 to "补全模式",
                        2 to "内容模式",
                    )
                    val selectedLabel = behaviorOptions.firstOrNull { it.first == characterNamesBehavior }?.second ?: "默认"

                    FormRow(label = "角色名称模式") {
                        Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .clickable { /* TODO: dropdown */ }
                                .padding(vertical = 8.dp),
                        )
                        // Simple dropdown via selection chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            behaviorOptions.forEach { (value, label) ->
                                FilterChip(
                                    selected = characterNamesBehavior == value,
                                    onClick = { onUpdateCharacterNamesBehavior(value) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Regex scripts card (navigates to full list page) ──
        item("regex_scripts") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToRegexList() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "预设正则脚本",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "共 ${regexScripts.size} 条脚本",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    TextButton(onClick = { onNavigateToRegexList() }) {
                        Text("管理")
                    }
                }
            }
        }

        item("config_switches") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SectionTitle("其他设置")

                    ConfigSwitch(
                        label = "续写预填充",
                        description = "\"续写\"将以助手身份发送最后一条消息，而不是发送带有指令的系统消息。",
                        checked = continuePrefill,
                        onCheckedChange = onUpdateContinuePrefill,
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    ConfigSwitch(
                        label = "压缩系统消息",
                        description = "将连续的系统消息合并为一条（不包括示例对话），可能会提高一些模型的连贯性。",
                        checked = squashSystemMessages,
                        onCheckedChange = onUpdateSquashSystemMessages,
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    ConfigSwitch(
                        label = "启用函数调用",
                        description = "允许使用功能工具。可以被各种扩展利用来提供附加功能。当提示词后处理没有选择工具时不支持。",
                        checked = functionCalling,
                        onCheckedChange = onUpdateFunctionCalling,
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    ConfigSwitch(
                        label = "发送内联媒体",
                        description = "以 base64 格式内联发送媒体文件。",
                        checked = sendInlineMedia,
                        onCheckedChange = onUpdateSendInlineMedia,
                    )
                }
            }
        }
    }
}

// ── Sub-pages ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetParamsPage(
    presetId: String,
    vm: PresetDetailVM = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
    ),
) {
    val navController = LocalNavController.current
    val hasUnsavedChanges by vm.hasUnsavedChanges.collectAsStateWithLifecycle()
    val modelParams by vm.modelParams.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(presetId) {
        vm.loadPreset(presetId)
    }

    BackHandler(enabled = hasUnsavedChanges) {
        scope.launch {
            vm.save()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("参数") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            if (hasUnsavedChanges) vm.save()
                            navController.popBackStack()
                        }
                    }) {
                        Icon(HugeIcons.ArrowLeft01, contentDescription = null)
                    }
                },
                colors = CustomColors.topBarColors,
            )
        },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        ParamsTab(
            modelParams = modelParams,
            onUpdateTemperature = { vm.updateTemperature(it) },
            onUpdateTopP = { vm.updateTopP(it) },
            onUpdateTopK = { vm.updateTopK(it) },
            onUpdateMinP = { vm.updateMinP(it) },
            onUpdateTopA = { vm.updateTopA(it) },
            onUpdateRepetitionPenalty = { vm.updateRepetitionPenalty(it) },
            onUpdateFrequencyPenalty = { vm.updateFrequencyPenalty(it) },
            onUpdatePresencePenalty = { vm.updatePresencePenalty(it) },
            onUpdateSeed = { vm.updateSeed(it) },
            onUpdateN = { vm.updateN(it) },
            onUpdateMaxTokens = { vm.updateMaxTokens(it) },
            onUpdateMaxContext = { vm.updateMaxContext(it) },
            onUpdateStop = { vm.updateStop(it) },
            onUpdateStreamOutput = { vm.updateStreamOutput(it) },
            onUpdateReasoningLevel = { vm.updateReasoningLevel(it) },
            innerPadding = innerPadding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetConfigPage(
    presetId: String,
    vm: PresetDetailVM = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
    ),
) {
    val navController = LocalNavController.current
    val hasUnsavedChanges by vm.hasUnsavedChanges.collectAsStateWithLifecycle()
    val modelParams by vm.modelParams.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(presetId) {
        vm.loadPreset(presetId)
    }

    BackHandler(enabled = hasUnsavedChanges) {
        scope.launch {
            vm.save()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            if (hasUnsavedChanges) vm.save()
                            navController.popBackStack()
                        }
                    }) {
                        Icon(HugeIcons.ArrowLeft01, contentDescription = null)
                    }
                },
                colors = CustomColors.topBarColors,
            )
        },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        ConfigTab(
            characterNamesBehavior = modelParams.characterNamesBehavior,
            continuePrefill = modelParams.continuePrefill,
            squashSystemMessages = modelParams.squashSystemMessages,
            functionCalling = modelParams.functionCalling,
            sendInlineMedia = modelParams.sendInlineMedia,
            regexScripts = modelParams.regexScripts,
            onUpdateCharacterNamesBehavior = { vm.updateCharacterNamesBehavior(it) },
            onUpdateContinuePrefill = { vm.updateContinuePrefill(it) },
            onUpdateSquashSystemMessages = { vm.updateSquashSystemMessages(it) },
            onUpdateFunctionCalling = { vm.updateFunctionCalling(it) },
            onUpdateSendInlineMedia = { vm.updateSendInlineMedia(it) },
            onNavigateToRegexList = { navController.navigate(Screen.PresetRegexScriptList(presetId)) },
            innerPadding = innerPadding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetPromptTemplatePage(
    presetId: String,
    vm: PresetDetailVM = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
    ),
) {
    val navController = LocalNavController.current
    val hasUnsavedChanges by vm.hasUnsavedChanges.collectAsStateWithLifecycle()
    val modelParams by vm.modelParams.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(presetId) {
        vm.loadPreset(presetId)
    }

    BackHandler(enabled = hasUnsavedChanges) {
        scope.launch {
            vm.save()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("提示词模板") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            if (hasUnsavedChanges) vm.save()
                            navController.popBackStack()
                        }
                    }) {
                        Icon(HugeIcons.ArrowLeft01, contentDescription = null)
                    }
                },
                colors = CustomColors.topBarColors,
            )
        },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        TemplatesTab(
            modelParams = modelParams,
            vm = vm,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatFieldCard(
    label: String,
    variable: String,
    value: String,
    onValueChange: (String) -> Unit,
    defaultValue: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(label)
                SuggestionChip(
                    onClick = { onValueChange(defaultValue) },
                    label = { Text(variable, style = MaterialTheme.typography.labelSmall) },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                minLines = 3,
                maxLines = 6,
                placeholder = { Text("输入格式模板，使用 ${variable} 引用内容") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptFieldCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    defaultValue: String,
    placeholder: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(label)
                TextButton(
                    onClick = { onValueChange(defaultValue) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text("恢复默认", style = MaterialTheme.typography.labelSmall)
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                minLines = 3,
                maxLines = 8,
                placeholder = { Text(placeholder) },
                textStyle = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ConfigSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                checkedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                checkedBorderColor = MaterialTheme.colorScheme.outline,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

// ── Shared form row helper ──

@Composable
private fun ParamCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun CompactDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun FormRowCompact(
    label: String,
    tail: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            tail?.invoke()
        }
        content()
    }
}

@Composable
private fun FormRow(
    label: String,
    tail: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            tail?.invoke()
        }
        content()
    }
}
