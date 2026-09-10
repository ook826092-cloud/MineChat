package cn.mine.minestars.feature.tavern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.mine.minestars.Screen
import cn.mine.minestars.ui.components.ui.BottomCreateToolbar
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.modifier.onClick
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.plus
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowLeft01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.MoreVertical
import me.rerere.hugeicons.stroke.ToggleOff
import org.koin.androidx.compose.koinViewModel
import org.json.JSONObject

@Composable
fun AssistantWorldBookListPage(
    assistantId: String,
    vm: AssistantWorldBookListVM = koinViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = LocalNavController.current
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(assistantId) { vm.loadAssistant(assistantId) }

    // Derive world book data from assistant
    val worldBookJson = assistant?.worldBookJson
    val hasWorldBook = !worldBookJson.isNullOrBlank()
    val worldBookName = if (hasWorldBook) {
        runCatching { JSONObject(worldBookJson).optString("name", "世界书") }.getOrNull() ?: "世界书"
    } else ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("角色世界书") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(HugeIcons.ArrowLeft01, contentDescription = null)
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(HugeIcons.MoreVertical, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if ((!hasWorldBook) || (runCatching { JSONObject(worldBookJson).optBoolean("enabled", true) }.getOrNull() != false)) "关闭全部" else "启用全部") },
                            onClick = {
                                val isEnabled = runCatching { JSONObject(worldBookJson).optBoolean("enabled", true) }.getOrNull() ?: true
                                vm.toggleAllWorldBooks(!isEnabled)
                                showMenu = false
                            },
                        )
                    }
                },
                colors = CustomColors.topBarColors,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (!hasWorldBook) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无角色世界书",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp) + PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(listOf(assistantId), key = { it }) {
                        Card(
                            onClick = { navController.navigate(Screen.AssistantWorldBookDetail(assistantId)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = worldBookName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "世界书",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                                IconButton(onClick = {
                                    vm.toggleWorldBookEnabled()
                                }) {
                                    Icon(
                                        imageVector = HugeIcons.MoreVertical,
                                        contentDescription = "更多",
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BottomCreateToolbar(
                text = "添加条目",
                expanded = !hasWorldBook || (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0),
                onClick = {
                    scope.launch {
                        vm.ensureWorldBook()
                        navController.navigate(Screen.AssistantWorldBookDetail(assistantId))
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除世界书") },
            text = { Text("确定删除该世界书吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteWorldBook()
                    showDeleteDialog = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}
