package cn.mine.minestars.ui.pages.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.File02
import me.rerere.hugeicons.stroke.PencilEdit01
import cn.mine.minestars.R
import cn.mine.minestars.Screen
import cn.mine.minestars.core.workspace.WorkspaceShellStatus
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.components.ui.FormItem
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.ui.theme.extendColors
import cn.mine.minestars.utils.plus
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkspacePage(
    vm: WorkspaceVM = koinViewModel(),
) {
    val workspaces by vm.workspaces.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<cn.mine.minestars.data.db.entity.WorkspaceEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<cn.mine.minestars.data.db.entity.WorkspaceEntity?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.workspace_page_title)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(HugeIcons.Add01, contentDescription = stringResource(R.string.workspace_page_create))
            }
        },
        containerColor = CustomColors.topBarColors.containerColor,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        if (workspaces.isEmpty()) {
            EmptyWorkspaceState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding + PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(workspaces, key = { it.id }) { workspace ->
                    WorkspaceCard(
                        workspace = workspace,
                        onClick = { navController.navigate(Screen.WorkspaceDetail(workspace.id)) },
                        onRename = { renameTarget = workspace },
                        onDelete = { deleteTarget = workspace },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateWorkspaceDialog(
            title = stringResource(R.string.workspace_page_create_new),
            initialName = "",
            existingNames = workspaces.map { it.name.trim() }.toSet(),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                vm.createWorkspace(name)
                showCreateDialog = false
            },
        )
    }

    renameTarget?.let { target ->
        RenameWorkspaceDialog(
            currentName = target.name,
            onDismiss = { renameTarget = null },
            onRename = { newName ->
                vm.renameWorkspace(target.id, newName)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.workspace_page_delete_title)) },
            text = { Text(stringResource(R.string.workspace_page_delete_confirm, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteWorkspace(target)
                    deleteTarget = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun WorkspaceCard(
    workspace: cn.mine.minestars.data.db.entity.WorkspaceEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val shellStatus = runCatching {
        WorkspaceShellStatus.valueOf(workspace.shellStatus)
    }.getOrDefault(WorkspaceShellStatus.DISABLED)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShellStatusDot(shellStatus)
            Spacer(Modifier.size(12.dp))
            Icon(
                imageVector = HugeIcons.File02,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workspace.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = workspace.shellStatus.toShellStatusLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRename) {
                Icon(HugeIcons.PencilEdit01, contentDescription = "重命名")
            }
            IconButton(onClick = onDelete) {
                Icon(HugeIcons.Delete01, contentDescription = "删除")
            }
        }
    }
}

@Composable
private fun CreateWorkspaceDialog(
    title: String,
    initialName: String,
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val trimmedName = name.trim()
    val isDuplicate = trimmedName.isNotEmpty() && trimmedName in existingNames

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.workspace_page_name)) },
                singleLine = true,
                isError = isDuplicate,
                supportingText = if (isDuplicate) {
                    { Text(stringResource(R.string.workspace_page_name_duplicate)) }
                } else null,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmedName) },
                enabled = name.isNotBlank() && !isDuplicate,
            ) {
                Text(stringResource(R.string.workspace_page_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun RenameWorkspaceDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workspace_page_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.workspace_page_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(name) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.workspace_page_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun EmptyWorkspaceState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = HugeIcons.File02,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.workspace_page_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.workspace_page_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun String.toShellStatusLabel(): String = when (this) {
    WorkspaceShellStatus.DISABLED.name -> stringResource(R.string.workspace_page_shell_disabled)
    WorkspaceShellStatus.INSTALLING.name -> stringResource(R.string.workspace_page_shell_installing)
    WorkspaceShellStatus.READY.name -> stringResource(R.string.workspace_page_shell_ready)
    WorkspaceShellStatus.BROKEN.name -> stringResource(R.string.workspace_page_shell_broken)
    else -> lowercase()
}

@Composable
private fun ShellStatusDot(status: WorkspaceShellStatus) {
    val color = when (status) {
        WorkspaceShellStatus.READY -> MaterialTheme.extendColors.green6
        WorkspaceShellStatus.INSTALLING -> MaterialTheme.extendColors.orange5
        WorkspaceShellStatus.BROKEN -> MaterialTheme.extendColors.red6
        WorkspaceShellStatus.DISABLED -> MaterialTheme.extendColors.gray5
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}
