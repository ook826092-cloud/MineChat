package cn.mine.minestars.ui.components.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowRight01
import me.rerere.hugeicons.stroke.Codesandbox
import me.rerere.hugeicons.stroke.Tick02
import cn.mine.minestars.R
import cn.mine.minestars.core.workspace.WorkspaceShellStatus
import cn.mine.minestars.data.db.entity.WorkspaceEntity
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.ui.theme.extendColors

@Composable
internal fun WorkspaceSelectSheet(
    assistant: Assistant,
    workspaces: List<WorkspaceEntity>,
    onSelect: (String?) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.files_picker_select_workspace),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WorkspaceSelectRow(
                    title = stringResource(R.string.files_picker_no_bind),
                    selected = assistant.workspaceId == null,
                    onClick = { onSelect(null) },
                )
                workspaces.forEach { workspace ->
                    val shellStatus = runCatching {
                        WorkspaceShellStatus.valueOf(workspace.shellStatus)
                    }.getOrDefault(WorkspaceShellStatus.DISABLED)
                    WorkspaceSelectRow(
                        title = workspace.name,
                        status = workspace.shellStatus.toShellStatusLabelRes(),
                        statusColor = shellStatus.toStatusColor(),
                        selected = workspace.id == assistant.workspaceId,
                        onClick = { onSelect(workspace.id) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            ListItem(
                leadingContent = {
                    Icon(HugeIcons.Codesandbox, contentDescription = null)
                },
                headlineContent = {
                    Text(stringResource(R.string.extensions_page_workspace))
                },
                trailingContent = {
                    Icon(
                        imageVector = HugeIcons.ArrowRight01,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .clickable { onManage() },
            )
        }
    }
}

@Composable
private fun WorkspaceSelectRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    status: String? = null,
    statusColor: Color = MaterialTheme.extendColors.gray5,
) {
    ListItem(
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.size(8.dp))
                Icon(HugeIcons.Codesandbox, contentDescription = null)
            }
        },
        headlineContent = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = status?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = if (selected) {
            {
                Icon(
                    imageVector = HugeIcons.Tick02,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else null,
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                Color.Transparent
            }
        ),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() },
    )
}

@Composable
private fun String.toShellStatusLabelRes(): String = when (this) {
    WorkspaceShellStatus.DISABLED.name -> stringResource(R.string.workspace_page_shell_disabled)
    WorkspaceShellStatus.INSTALLING.name -> stringResource(R.string.workspace_page_shell_installing)
    WorkspaceShellStatus.READY.name -> stringResource(R.string.workspace_page_shell_ready)
    WorkspaceShellStatus.BROKEN.name -> stringResource(R.string.workspace_page_shell_broken)
    else -> lowercase()
}

@Composable
private fun WorkspaceShellStatus.toStatusColor(): Color = when (this) {
    WorkspaceShellStatus.READY -> MaterialTheme.extendColors.green6
    WorkspaceShellStatus.INSTALLING -> MaterialTheme.extendColors.orange5
    WorkspaceShellStatus.BROKEN -> MaterialTheme.extendColors.red6
    WorkspaceShellStatus.DISABLED -> MaterialTheme.extendColors.gray5
}
