package cn.mine.minestars.feature.tavern

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.mine.minestars.data.db.entity.RegexScriptEntity
import cn.mine.minestars.Screen
import me.rerere.hugeicons.stroke.ArrowLeft01
import cn.mine.minestars.ui.components.ui.BottomCreateToolbar
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.modifier.onClick
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.plus
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Edit01
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.DragDropHorizontal

@Composable
fun RegexGroupDetailPage(
    groupId: String,
    vm: RegexGroupDetailVM = koinViewModel(),
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val group by vm.group.collectAsStateWithLifecycle()
    val scripts by vm.scripts.collectAsStateWithLifecycle()
    val hasUnsavedChanges by vm.hasUnsavedChanges.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var actionSheetScript by remember { mutableStateOf<RegexScriptEntity?>(null) }

    BackHandler(enabled = hasUnsavedChanges) {
        scope.launch {
            vm.save()
            navController.popBackStack()
        }
    }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        vm.reorderScripts(from.index, to.index)
    }

    LaunchedEffect(groupId) { vm.loadGroup(groupId) }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is RegexGroupDetailEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is RegexGroupDetailEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        group?.name?.ifBlank { "正则组" } ?: "正则组",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        if (group == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "正则组不存在或已删除",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp) + PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (scripts.isEmpty()) {
                    item("empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "暂无正则表达式",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                } else {
                    itemsIndexed(scripts, key = { _, s -> s.id }) { index, script ->
                        ReorderableItem(
                            state = reorderableState,
                            key = script.id,
                        ) { isDragging ->
                            ScriptCardSwipeable(
                                script = script,
                                onToggleEnabled = { vm.toggleScriptEnabled(script.id) },
                                onDelete = { vm.deleteScript(script.id) },
                                onClick = { navController.navigate(Screen.RegexScriptDetail(script.id)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
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
            }

            val expanded = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

            BottomCreateToolbar(
                text = "添加正则",
                expanded = expanded,
                onClick = { vm.addScript() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }

    // ── Script action sheet ──
    actionSheetScript?.let { script ->
        ModalBottomSheet(
            onDismissRequest = { actionSheetScript = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = script.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ListItem(
                    headlineContent = { Text("编辑") },
                    leadingContent = {
                        Icon(HugeIcons.Edit01, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.onClick {
                        actionSheetScript = null
                        navController.navigate(Screen.RegexScriptDetail(script.id))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )

                ListItem(
                    headlineContent = { Text("复制") },
                    leadingContent = {
                        Icon(HugeIcons.Copy01, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.onClick {
                        vm.copyScript(script.id)
                        actionSheetScript = null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

/**
 * Wraps ScriptCard with SwipeToDismissBox (left-swipe reveals actions).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptCardSwipeable(
    script: RegexScriptEntity,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()

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
                IconButton(onClick = { scope.launch { swipeState.reset() } }) {
                    Icon(HugeIcons.Cancel01, null)
                }
                FilledIconButton(
                    onClick = {
                        scope.launch { onDelete(); swipeState.reset() }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(HugeIcons.Delete01, "删除")
                }
            }
        },
        enableDismissFromStartToEnd = false,
        modifier = modifier,
    ) {
        ScriptCard(
            script = script,
            onToggleEnabled = onToggleEnabled,
            onClick = onClick,
        )
    }
}

@Composable
private fun ScriptCard(
    script: RegexScriptEntity,
    onToggleEnabled: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val disabled = runCatching { JSONObject(script.rawJson).optBoolean("disabled", false) }.getOrDefault(false)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
                imageVector = HugeIcons.DragDropHorizontal,
                contentDescription = "拖动排序",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = script.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = script.summaryText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Switch(
                checked = !disabled,
                onCheckedChange = { onToggleEnabled() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                    checkedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    checkedBorderColor = MaterialTheme.colorScheme.outline,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun RegexScriptEntity.summaryText(): String {
    val json = runCatching { JSONObject(rawJson) }.getOrNull()
    val disabled = json?.optBoolean("disabled", false) ?: false
    val substituteRegex = json?.optInt("substituteRegex", json.optInt("substitute_regex", 0)) ?: 0
    val placement = json?.optJSONArray("placement")
    val placementText = if (placement == null || placement.length() == 0) {
        "默认"
    } else {
        (0 until placement.length()).joinToString("/") { index ->
            when (placement.optInt(index)) {
                1 -> "输入"
                2 -> "输出"
                3 -> "命令"
                5 -> "世界书"
                6 -> "推理"
                else -> placement.optInt(index).toString()
            }
        }
    }
    val macroText = when (substituteRegex) {
        1 -> "宏:原样"
        2 -> "宏:转义"
        else -> "宏:关闭"
    }
    val stateText = if (disabled) "关闭" else "启用"
    return "$stateText · $scope · $placementText · $macroText"
}
