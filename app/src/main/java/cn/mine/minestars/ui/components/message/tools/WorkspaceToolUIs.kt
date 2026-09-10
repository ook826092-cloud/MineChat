package cn.mine.minestars.ui.components.message.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.longOrNull
import cn.mine.common.http.jsonObjectOrNull
import cn.mine.highlight.CodeHighlightText
import cn.mine.minestars.ui.components.richtext.DiffAddedColor
import cn.mine.minestars.ui.components.richtext.DiffRemovedColor
import cn.mine.minestars.ui.components.richtext.DiffView
import cn.mine.minestars.ui.components.richtext.HighlightCodeBlock
import cn.mine.minestars.ui.components.richtext.parseDiffStats
import cn.mine.minestars.ui.modifier.shimmer
import cn.mine.minestars.utils.jsonPrimitiveOrNull
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowRight01
import me.rerere.hugeicons.stroke.Delete02
import me.rerere.hugeicons.stroke.FileAdd
import me.rerere.hugeicons.stroke.FileEdit
import me.rerere.hugeicons.stroke.FileExport
import me.rerere.hugeicons.stroke.FileView
import me.rerere.hugeicons.stroke.Folder01
import me.rerere.hugeicons.stroke.SourceCode

object EditFileToolUI : ToolUIRenderer {
    private const val SUMMARY_MAX_LINES = 10

    override val toolName: String = "workspace_edit_file"

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.FileEdit

    @Composable
    override fun title(context: ToolUIContext): String {
        val path = context.arguments.getStringContent("path")
        return if (path != null) "Edit: $path" else "Edit file"
    }

    private fun diffOf(context: ToolUIContext): String? {
        val path = context.arguments.getStringContent("path") ?: return null
        val oldText = context.arguments.getStringContent("old_text") ?: return null
        val newText = context.arguments.getStringContent("new_text") ?: return null
        return generateSimpleDiff(oldText, newText, path)
    }

    override fun hasSummary(context: ToolUIContext): Boolean = diffOf(context) != null

    @Composable
    override fun Summary(context: ToolUIContext) {
        val diff = remember(context) { diffOf(context) } ?: return
        val stats = remember(diff) { parseDiffStats(diff) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "+${stats.additions}",
                style = MaterialTheme.typography.labelSmall,
                color = DiffAddedColor,
            )
            Text(
                text = "-${stats.deletions}",
                style = MaterialTheme.typography.labelSmall,
                color = DiffRemovedColor,
            )
        }
        DiffView(
            diff = diff,
            modifier = Modifier.fillMaxWidth(),
            maxLines = SUMMARY_MAX_LINES,
            showFileHeader = false,
        )
    }

    @Composable
    override fun Preview(context: ToolUIContext, onDismissRequest: () -> Unit) {
        val diff = remember(context) { diffOf(context) }
        if (diff == null) {
            DefaultToolPreview(context = context)
            return
        }
        val stats = remember(diff) { parseDiffStats(diff) }
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = context.arguments.getStringContent("path") ?: toolName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "+${stats.additions}",
                    style = MaterialTheme.typography.labelMedium,
                    color = DiffAddedColor,
                )
                Text(
                    text = "-${stats.deletions}",
                    style = MaterialTheme.typography.labelMedium,
                    color = DiffRemovedColor,
                )
            }
            DiffView(
                diff = diff,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

object ReadFileToolUI : ToolUIRenderer {
    override val toolName: String = "workspace_read_file"

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.FileView

    @Composable
    override fun title(context: ToolUIContext): String {
        val path = context.arguments.getStringContent("path")
        return if (path != null) "Read: $path" else "Read file"
    }

    private fun textOf(context: ToolUIContext): String? =
        context.content.getStringContent("text")

    override fun hasSummary(context: ToolUIContext): Boolean = textOf(context) != null

    @Composable
    override fun Summary(context: ToolUIContext) {
        val text = remember(context) { textOf(context) } ?: return
        FileContentSummary(
            text = text,
            path = context.arguments.getStringContent("path"),
            loading = context.loading,
        )
    }

    @Composable
    override fun Preview(context: ToolUIContext, onDismissRequest: () -> Unit) {
        val text = remember(context) { textOf(context) }
        if (text == null) {
            DefaultToolPreview(context = context)
            return
        }
        FileContentPreview(path = context.arguments.getStringContent("path"), code = text)
    }
}

object WriteFileToolUI : ToolUIRenderer {
    override val toolName: String = "workspace_write_file"

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.FileAdd

    @Composable
    override fun title(context: ToolUIContext): String {
        val path = context.arguments.getStringContent("path")
        return if (path != null) "Write: $path" else "Write file"
    }

    private fun textOf(context: ToolUIContext): String? =
        context.arguments.getStringContent("text")

    override fun hasSummary(context: ToolUIContext): Boolean = textOf(context) != null

    @Composable
    override fun Summary(context: ToolUIContext) {
        val text = remember(context) { textOf(context) } ?: return
        FileContentSummary(
            text = text,
            path = context.arguments.getStringContent("path"),
            loading = context.loading,
        )
    }

    @Composable
    override fun Preview(context: ToolUIContext, onDismissRequest: () -> Unit) {
        val text = remember(context) { textOf(context) }
        if (text == null) {
            DefaultToolPreview(context = context)
            return
        }
        FileContentPreview(path = context.arguments.getStringContent("path"), code = text)
    }
}

private const val FILE_SUMMARY_MAX_LINES = 10

@Composable
private fun FileContentSummary(text: String, path: String?, loading: Boolean) {
    val preview = remember(text) {
        text.lineSequence().take(FILE_SUMMARY_MAX_LINES).joinToString("\n")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .shimmer(isLoading = loading),
    ) {
        CodeHighlightText(
            code = preview,
            language = languageOf(path),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = FILE_SUMMARY_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FileContentPreview(path: String?, code: String) {
    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = path ?: "file",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        HighlightCodeBlock(
            code = code,
            language = languageOf(path),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

object ListFilesToolUI : ToolUIRenderer {
    private const val SUMMARY_MAX_NAMES = 6

    override val toolName: String = "workspace_list_files"

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.Folder01

    @Composable
    override fun title(context: ToolUIContext): String {
        val path = context.arguments.getStringContent("path")
        return if (!path.isNullOrBlank()) "List: $path" else "List files"
    }

    private fun entries(context: ToolUIContext): List<JsonElement> =
        context.content?.jsonObjectOrNull?.get("entries")?.jsonArray ?: emptyList()

    override fun hasSummary(context: ToolUIContext): Boolean = context.content != null

    @Composable
    override fun Summary(context: ToolUIContext) {
        val entries = remember(context) { entries(context) }
        val names = remember(entries) { entries.mapNotNull { it.getStringContent("name") } }
        Text(
            text = if (names.isEmpty()) {
                "Empty"
            } else {
                val shown = names.take(SUMMARY_MAX_NAMES).joinToString(", ")
                val more = if (names.size > SUMMARY_MAX_NAMES) " …" else ""
                "${entries.size} items: $shown$more"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.shimmer(isLoading = context.loading),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }

    @Composable
    override fun Preview(context: ToolUIContext, onDismissRequest: () -> Unit) {
        val entries = remember(context) { entries(context) }
        if (entries.isEmpty()) {
            DefaultToolPreview(context = context)
            return
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(entries) { entry ->
                val isDir = entry.boolean("isDirectory") ?: false
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (isDir) HugeIcons.Folder01 else HugeIcons.FileView,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = entry.getStringContent("name") ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!isDir) {
                        Text(
                            text = formatBytes(entry.long("sizeBytes") ?: 0L),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

object DeleteFileToolUI : ToolUIRenderer {
    override val toolName: String = "workspace_delete_file"

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.Delete02

    @Composable
    override fun title(context: ToolUIContext): String {
        val path = context.arguments.getStringContent("path")
        return if (path != null) "Delete: $path" else "Delete file"
    }

    override fun hasSummary(context: ToolUIContext): Boolean = context.content != null

    @Composable
    override fun Summary(context: ToolUIContext) {
        val success = context.content.boolean("success") ?: return
        val path = context.content.getStringContent("path") ?: ""
        Text(
            text = if (success) "Deleted $path" else "Failed to delete $path",
            style = MaterialTheme.typography.labelSmall,
            color = if (success) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.error
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

object MoveFileToolUI : ToolUIRenderer {
    override val toolName: String = "workspace_move_file"

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.FileExport

    @Composable
    override fun title(context: ToolUIContext): String = "Move file"

    override fun hasSummary(context: ToolUIContext): Boolean =
        context.arguments.getStringContent("source") != null

    @Composable
    override fun Summary(context: ToolUIContext) {
        val source = context.arguments.getStringContent("source") ?: return
        val target = context.arguments.getStringContent("target") ?: return
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = HugeIcons.ArrowRight01,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = target,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

object ShellToolUI : ToolUIRenderer {
    private const val TITLE_MAX_CHARS = 40
    private const val SUMMARY_MAX_LINES = 8

    override val toolName: String = "workspace_shell"

    override fun icon(context: ToolUIContext): ImageVector = HugeIcons.SourceCode

    @Composable
    override fun title(context: ToolUIContext): String {
        val command = context.arguments.getStringContent("command") ?: return "Shell"
        val preview = command.replace("\n", " ").trim()
        val truncated = if (preview.length > TITLE_MAX_CHARS) preview.take(TITLE_MAX_CHARS) + "…" else preview
        return "Shell: $truncated"
    }

    override fun hasSummary(context: ToolUIContext): Boolean = context.content != null

    @Composable
    override fun Summary(context: ToolUIContext) {
        val content = context.content ?: return
        val combined = remember(content) {
            listOf(content.getStringContent("stdout"), content.getStringContent("stderr"))
                .filterNot { it.isNullOrBlank() }
                .joinToString("\n")
                .trim()
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ShellExitStatus(content, MaterialTheme.typography.labelSmall)
            if (combined.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .shimmer(isLoading = context.loading),
                ) {
                    Text(
                        text = combined.lineSequence().take(SUMMARY_MAX_LINES).joinToString("\n"),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = SUMMARY_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    @Composable
    override fun Preview(context: ToolUIContext, onDismissRequest: () -> Unit) {
        val content = context.content
        if (content == null) {
            DefaultToolPreview(context = context)
            return
        }
        val command = context.arguments.getStringContent("command").orEmpty()
        val cwd = context.arguments.getStringContent("cwd")
        val stdout = content.getStringContent("stdout").orEmpty()
        val stderr = content.getStringContent("stderr").orEmpty()
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Shell",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                ShellExitStatus(content, MaterialTheme.typography.labelMedium)
            }
            HighlightCodeBlock(
                code = if (cwd.isNullOrBlank()) command else "# cwd: $cwd\n$command",
                language = "bash",
                modifier = Modifier.fillMaxWidth(),
            )
            if (stdout.isNotEmpty()) {
                Text(text = "stdout", style = MaterialTheme.typography.labelMedium)
                HighlightCodeBlock(
                    code = stdout,
                    language = "plaintext",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (stderr.isNotEmpty()) {
                Text(
                    text = "stderr",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                HighlightCodeBlock(
                    code = stderr,
                    language = "plaintext",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ShellExitStatus(content: JsonElement, style: androidx.compose.ui.text.TextStyle) {
    val exitCode = content.int("exitCode")
    val timedOut = content.boolean("timedOut") ?: false
    val truncated = content.boolean("truncated") ?: false
    val ok = !timedOut && exitCode == 0
    Text(
        text = buildString {
            when {
                timedOut -> append("timeout")
                else -> append("exit ${exitCode ?: "?"}")
            }
            if (truncated) append(" (truncated)")
        },
        style = style,
        color = if (ok) DiffAddedColor else MaterialTheme.colorScheme.error,
    )
}

private fun JsonElement?.boolean(key: String): Boolean? =
    this?.jsonObjectOrNull?.get(key)?.jsonPrimitiveOrNull?.booleanOrNull

private fun JsonElement?.int(key: String): Int? =
    this?.jsonObjectOrNull?.get(key)?.jsonPrimitiveOrNull?.intOrNull

private fun JsonElement?.long(key: String): Long? =
    this?.jsonObjectOrNull?.get(key)?.jsonPrimitiveOrNull?.longOrNull

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

private fun languageOf(path: String?): String = when (
    path?.substringAfterLast('.', "")?.lowercase() ?: ""
) {
    "kt", "kts" -> "kotlin"
    "java" -> "java"
    "js", "mjs", "cjs" -> "javascript"
    "ts" -> "typescript"
    "tsx" -> "tsx"
    "jsx" -> "jsx"
    "py" -> "python"
    "rb" -> "ruby"
    "go" -> "go"
    "rs" -> "rust"
    "c", "h" -> "c"
    "cpp", "cc", "cxx", "hpp", "hxx" -> "cpp"
    "cs" -> "csharp"
    "swift" -> "swift"
    "php" -> "php"
    "sh", "bash", "zsh" -> "bash"
    "json" -> "json"
    "xml" -> "xml"
    "html", "htm" -> "html"
    "css" -> "css"
    "scss" -> "scss"
    "yaml", "yml" -> "yaml"
    "toml" -> "toml"
    "md", "markdown" -> "markdown"
    "sql" -> "sql"
    "gradle" -> "groovy"
    else -> "plaintext"
}

private fun generateSimpleDiff(oldText: String, newText: String, path: String): String? {
    if (oldText == newText) return null
    val oldLines = oldText.lines()
    val newLines = newText.lines()
    val sb = StringBuilder()
    sb.appendLine("--- a/$path")
    sb.appendLine("+++ b/$path")
    sb.appendLine("@@ -1,${oldLines.size} +1,${newLines.size} @@")
    for (line in oldLines) sb.appendLine("-$line")
    for (line in newLines) sb.appendLine("+$line")
    return sb.toString()
}
