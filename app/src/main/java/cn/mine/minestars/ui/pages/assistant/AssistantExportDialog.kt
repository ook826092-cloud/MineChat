package cn.mine.minestars.ui.pages.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.mine.minestars.R

data class ExportOptions(
    val format: ExportFormat = ExportFormat.JSON,
    val specVersion: SpecVersion = SpecVersion.V2,
    val includeWorldBook: Boolean = true,
    val includeRegex: Boolean = true,
)

enum class ExportFormat { JSON, PNG }
enum class SpecVersion { V2, V3 }

@Composable
fun AssistantExportDialog(
    hasWorldBook: Boolean,
    hasRegexScripts: Boolean,
    onDismiss: () -> Unit,
    onExport: (ExportOptions) -> Unit,
) {
    var options by remember { mutableStateOf(ExportOptions()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(stringResource(R.string.assistant_export_title), style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // File format
                Text(stringResource(R.string.assistant_export_format), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExportFormat.entries.forEach { format ->
                        ExportChip(
                            text = format.name,
                            selected = options.format == format,
                            onClick = { options = options.copy(format = format) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Spec version
                Text(stringResource(R.string.assistant_export_spec_version), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SpecVersion.entries.forEach { version ->
                        ExportChip(
                            text = version.name,
                            selected = options.specVersion == version,
                            onClick = { options = options.copy(specVersion = version) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ── 附加数据开关 ──
                if (hasWorldBook || hasRegexScripts) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (hasWorldBook) {
                            ExportChip(
                                text = stringResource(R.string.assistant_export_include_world_book),
                                selected = options.includeWorldBook,
                                onClick = {
                                    options = options.copy(includeWorldBook = !options.includeWorldBook)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (hasRegexScripts) {
                            ExportChip(
                                text = stringResource(R.string.assistant_export_include_regex),
                                selected = options.includeRegex,
                                onClick = {
                                    options = options.copy(includeRegex = !options.includeRegex)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (hasWorldBook != hasRegexScripts) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onExport(options) }) {
                Text(stringResource(R.string.assistant_export_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.assistant_export_cancel))
            }
        },
    )
}

@Composable
private fun ExportChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
