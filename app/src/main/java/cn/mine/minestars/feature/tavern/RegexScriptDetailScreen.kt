package cn.mine.minestars.feature.tavern

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.context.Navigator
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.plus
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowLeft01
import org.json.JSONArray
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel

/**
 * Detail page for editing a regex script within a RegexGroup (Layer 3 global).
 * Thin wrapper around [RegexScriptEditorBody] with RegexGroupDetailVM binding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexScriptDetailPage(
    scriptId: String,
    groupVm: RegexGroupDetailVM = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
    ),
) {
    val navController = LocalNavController.current
    val scripts by groupVm.scripts.collectAsStateWithLifecycle()
    val script = scripts.find { it.id == scriptId }

    val initialRawJson = script?.rawJson ?: return

    RegexScriptEditorBody(
        scriptId = scriptId,
        initialRawJson = initialRawJson,
        scriptTitle = runCatching { JSONObject(initialRawJson).optString("scriptName", "") }.getOrDefault(""),
        onSave = { id, rawJson -> groupVm.updateScriptRawJson(id, rawJson) },
        navController = navController,
    )
}

/**
 * Reusable form content for tavern-format regex scripts.
 * Embeds all fields in a LazyColumn with a save button at the bottom.
 * Used inside [RegexScriptEditorBody] (page mode) and preset BottomSheet.
 */
@Composable
fun RegexScriptEditorContent(
    scriptId: String,
    initialRawJson: String,
    scriptTitle: String,
    onSave: (scriptId: String, rawJson: String) -> Unit,
    onClose: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val initialJson = remember(initialRawJson) {
        runCatching { JSONObject(initialRawJson) }.getOrNull() ?: JSONObject()
    }

    // ── Local editable state ──
    var scriptName by remember(scriptId, scriptTitle) { mutableStateOf(initialJson.optString("scriptName", "")) }
    var findRegex by remember(scriptId, scriptTitle) { mutableStateOf(initialJson.optString("findRegex", "")) }
    var replaceString by remember(scriptId, scriptTitle) { mutableStateOf(initialJson.optString("replaceString", "")) }
    var disabled by remember(scriptId, scriptTitle) { mutableStateOf(initialJson.optBoolean("disabled", false)) }
    var runOnEdit by remember(scriptId, scriptTitle) { mutableStateOf(initialJson.optBoolean("runOnEdit", true)) }
    var markdownOnly by remember(scriptId, scriptTitle) { mutableStateOf(initialJson.optBoolean("markdownOnly", false)) }
    var promptOnly by remember(scriptId, scriptTitle) { mutableStateOf(initialJson.optBoolean("promptOnly", false)) }
    var substituteRegex by remember(scriptId, scriptTitle) { mutableStateOf(initialJson.optInt("substituteRegex", 0)) }
    var minDepth by remember(scriptId, scriptTitle) {
        mutableStateOf(if (initialJson.has("minDepth") && !initialJson.isNull("minDepth"))
            initialJson.optInt("minDepth").toString() else "")
    }
    var maxDepth by remember(scriptId, scriptTitle) {
        mutableStateOf(if (initialJson.has("maxDepth") && !initialJson.isNull("maxDepth"))
            initialJson.optInt("maxDepth").toString() else "")
    }

    // trimStrings: JSONArray → newline-separated string
    var trimStrings by remember(scriptId, scriptTitle) {
        val arr = initialJson.optJSONArray("trimStrings")
            ?: initialJson.optJSONArray("trim_strings")
        mutableStateOf(if (arr != null) {
            (0 until arr.length()).joinToString("\n") { arr.optString(it) }
        } else "")
    }

    // placement: JSONArray → Set<Int>
    var placement by remember(scriptId, scriptTitle) {
        val arr = initialJson.optJSONArray("placement")
        mutableStateOf(if (arr != null) {
            (0 until arr.length()).map { arr.optInt(it) }.toSet()
        } else emptySet())
    }

    fun buildRawJson(): String {
        val json = JSONObject()
        json.put("scriptName", scriptName)
        json.put("findRegex", findRegex)
        json.put("replaceString", replaceString)
        json.put("disabled", disabled)
        json.put("runOnEdit", runOnEdit)
        json.put("markdownOnly", markdownOnly)
        json.put("promptOnly", promptOnly)
        json.put("substituteRegex", substituteRegex)
        if (minDepth.isNotBlank()) json.put("minDepth", minDepth.toIntOrNull() ?: JSONObject.NULL)
        else json.put("minDepth", JSONObject.NULL)
        if (maxDepth.isNotBlank()) json.put("maxDepth", maxDepth.toIntOrNull() ?: JSONObject.NULL)
        else json.put("maxDepth", JSONObject.NULL)
        json.put("placement", JSONArray(placement.sorted()))
        val trimLines = trimStrings.lines().map { it.trim() }.filter { it.isNotBlank() }
        json.put("trimStrings", if (trimLines.isNotEmpty()) JSONArray(trimLines) else JSONArray())
        return json.toString()
    }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        item { SectionTitle("基本信息") }
        item { RegexTextField("脚本名称", scriptName) { scriptName = it } }
        item { RegexTextField("查找正则", findRegex, singleLine = true) { findRegex = it } }
        item { RegexTextField("替换为", replaceString, minLines = 3) { replaceString = it } }
        item { RegexTextField("修剪字符串（每行一个）", trimStrings, minLines = 2) { trimStrings = it } }

        item { SectionTitle("影响范围") }
        item { PlacementCheckboxes(placement) { value -> placement = if (value in placement) placement - value else placement + value } }

        item { SectionTitle("深度过滤") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RegexTextField("最小深度", minDepth, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { minDepth = it }
                RegexTextField("最大深度", maxDepth, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { maxDepth = it }
            }
        }

        item { SectionTitle("选项") }
        item { RegexToggleField("编辑时运行", runOnEdit) { runOnEdit = it } }
        item { RegexToggleField("仅格式显示", markdownOnly) { markdownOnly = it } }
        item { RegexToggleField("仅格式提示", promptOnly) { promptOnly = it } }
        item {
            RegexDropdownField(
                label = "查找正则中的宏",
                value = substituteRegex,
                options = listOf(
                    0 to "不替换",
                    1 to "替换（原始）",
                    2 to "替换（转义）",
                ),
                onValueChange = { substituteRegex = it },
            )
        }

        // Save button at the bottom
        item {
            Button(
                onClick = { onSave(scriptId, buildRawJson()); onClose() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text("保存")
            }
        }
    }
}

/**
 * Full-screen page wrapper for tavern-format regex scripts.
 * Used by both [RegexScriptDetailPage] (Layer 3 global) and
 * [AssistantRegexScriptDetailPage] (Layer 2 assistant).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexScriptEditorBody(
    scriptId: String,
    initialRawJson: String,
    scriptTitle: String,
    onSave: (scriptId: String, rawJson: String) -> Unit,
    navController: Navigator,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    val name = remember(initialRawJson) {
                        runCatching { JSONObject(initialRawJson).optString("scriptName", "") }.getOrDefault("")
                    }
                    Text(name.ifBlank { "正则表达式" })
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(HugeIcons.ArrowLeft01, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        RegexScriptEditorContent(
            scriptId = scriptId,
            initialRawJson = initialRawJson,
            scriptTitle = scriptTitle,
            onSave = onSave,
            onClose = { navController.popBackStack() },
            contentPadding = innerPadding + PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize(),
        )
    }

    BackHandler { navController.popBackStack() }
}

// ── Shared helper composables ──

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
internal fun RegexTextField(
    label: String,
    value: String,
    minLines: Int = 1,
    singleLine: Boolean = false,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = if (minLines > 1) 12 else 1,
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors(),
    )
}

@Composable
internal fun RegexToggleField(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = switchColors())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RegexDropdownField(
    label: String,
    value: Int,
    options: List<Pair<Int, String>>,
    onValueChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == value }?.second ?: value.toString()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onValueChange(key); expanded = false })
            }
        }
    }
}

@Composable
internal fun PlacementCheckboxes(
    current: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    val options = listOf(
        1 to "用户输入",
        2 to "AI 输出",
        3 to "斜杠命令",
        5 to "世界信息",
        6 to "推理",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.chunked(2).forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chunk.forEach { (value, label) ->
                    SelectChip(
                        text = label,
                        selected = value in current,
                        onClick = { onToggle(value) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (chunk.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun SelectChip(
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

@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.outline,
    unfocusedLabelColor = MaterialTheme.colorScheme.outline,
)

@Composable
internal fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onSurface,
    checkedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    checkedBorderColor = MaterialTheme.colorScheme.outline,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
)

/** Parse trimStrings JSONArray to newline-separated string for comparison. */
internal fun parseTrimStrings(json: JSONObject): String {
    val arr = json.optJSONArray("trimStrings") ?: json.optJSONArray("trim_strings") ?: return ""
    return (0 until arr.length()).joinToString("\n") { arr.optString(it) }
}

/** Parse placement JSONArray to Set<Int> for comparison. */
internal fun parsePlacement(json: JSONObject): Set<Int> {
    val arr = json.optJSONArray("placement") ?: return emptySet()
    return (0 until arr.length()).map { arr.optInt(it) }.toSet()
}
