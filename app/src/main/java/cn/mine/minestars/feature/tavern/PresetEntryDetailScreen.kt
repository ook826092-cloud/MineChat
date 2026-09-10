package cn.mine.minestars.feature.tavern

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.mine.minestars.data.db.entity.PresetEntryEntity
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.plus
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowLeft01
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEntryDetailPage(
    presetId: String,
    entryIndex: Int,
) {
    val vm: PresetDetailVM = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
    )
    val navController = LocalNavController.current
    val entries by vm.entries.collectAsStateWithLifecycle()
    val entry = entries.find { it.entryIndex == entryIndex }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isReadOnly = entry?.marker == true

    var name by remember(entry?.id) { mutableStateOf(entry?.name.orEmpty()) }
    var content by remember(entry?.id) { mutableStateOf(entry?.content.orEmpty()) }
    var role by remember(entry?.id) { mutableStateOf(entry?.role.orEmpty()) }
    var injectionPosition by remember(entry?.id) { mutableStateOf(entry?.injectionPosition ?: 0) }
    var injectionDepth by remember(entry?.id) { mutableStateOf((entry?.injectionDepth ?: 4).toString()) }
    var injectionOrder by remember(entry?.id) { mutableStateOf((entry?.injectionOrder ?: 100).toString()) }
    var forbidOverrides by remember(entry?.id) { mutableStateOf(entry?.forbidOverrides ?: false) }
    var triggerText by remember(entry?.id) { mutableStateOf("") }
    var selectedTab by remember(entry?.id) { mutableStateOf(0) }

    val saveEntryAndGoBack: () -> Unit = {
        if (entry != null && !isReadOnly) {
            val updated = PresetEntryEntity(
                presetId = presetId,
                entryIndex = entry.entryIndex,
                id = entry.id,
                identifier = entry.identifier,
                name = name.trim().ifBlank { entry.name },
                enabled = entry.enabled,
                role = role,
                content = content,
                injectionPosition = injectionPosition,
                injectionDepth = injectionDepth.trim().toIntOrNull(),
                injectionOrder = injectionOrder.trim().toIntOrNull(),
                systemPrompt = entry.systemPrompt,
                marker = entry.marker,
                forbidOverrides = forbidOverrides,
                injectionTriggerJson = serializeInjectionTriggers(triggerText),
                mounted = entry.mounted,
            )
            vm.updateEntry(entry.entryIndex, updated)
        }
        navController.popBackStack()
    }

    BackHandler {
        saveEntryAndGoBack()
    }

    LaunchedEffect(entry?.id) {
        if (entry != null) {
            name = entry.name
            content = entry.content
            role = entry.role
            injectionPosition = entry.injectionPosition ?: 0
            injectionDepth = (entry.injectionDepth ?: 4).toString()
            injectionOrder = (entry.injectionOrder ?: 100).toString()
            forbidOverrides = entry.forbidOverrides
            triggerText = parseInjectionTriggers(entry.injectionTriggerJson)
        }
    }

    LaunchedEffect(presetId) {
        vm.loadPreset(presetId)
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = entry?.name ?: "条目详情",
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = saveEntryAndGoBack) {
                        Icon(HugeIcons.ArrowLeft01, contentDescription = null)
                    }
                },
                actions = {},
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        if (entry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "条目不存在或已删除",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            LazyColumn(
                contentPadding = innerPadding + PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    EntryTabRow(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                )
            }

                    if (isReadOnly) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "标记条目不可编辑",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                item {
                    PresetLineField(
                        label = "名称",
                        value = name,
                        readOnly = isReadOnly,
                        onValueChange = { name = it },
                    )
                }
                item {
                    PresetLineField(
                        label = "注入内容",
                        value = content,
                        minLines = 6,
                        readOnly = isReadOnly,
                        placeholder = builtinPlaceholder(entry),
                        onValueChange = { content = it },
                    )
                }
            }

            if (selectedTab == 1) {
                item {
                    PresetDropdownField(
                        label = "注入角色",
                        value = role,
                        options = listOf(
                            "system" to "系统",
                            "user" to "用户",
                            "assistant" to "助手",
                        ),
                        enabled = !isReadOnly,
                        onValueChange = { role = it },
                    )
                }
                item {
                    PresetDropdownField(
                        label = "注入位置",
                        value = injectionPosition.toString(),
                        options = listOf(
                            "0" to "相对",
                            "1" to "对话内",
                        ),
                        enabled = !isReadOnly,
                        onValueChange = { value ->
                            injectionPosition = value.toIntOrNull() ?: injectionPosition
                        },
                    )
                }
                item {
                    PresetLineField(
                        label = "注入深度",
                        value = injectionDepth,
                        readOnly = isReadOnly,
                        keyboardType = KeyboardType.Number,
                        onValueChange = { injectionDepth = it },
                    )
                }
                item {
                    PresetLineField(
                        label = "注入顺序",
                        value = injectionOrder,
                        readOnly = isReadOnly,
                        keyboardType = KeyboardType.Number,
                        onValueChange = { injectionOrder = it },
                    )
                }
            }

            if (selectedTab == 2) {
                item {
                    PresetLineField(
                        label = "触发器（用逗号分隔）",
                        value = triggerText,
                        readOnly = isReadOnly,
                        onValueChange = { triggerText = it },
                    )
                }
                item {
                    PresetToggleField(
                        label = "禁止覆盖",
                        checked = forbidOverrides,
                        enabled = !isReadOnly,
                        onCheckedChange = { forbidOverrides = it },
                    )
                }
            }
        }
    }

}
}

@Composable
private fun EntryTabRow(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val tabs = listOf("基础", "激活", "其他")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEachIndexed { index, title ->
            val active = selected == index
            Button(
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active)
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    else Color.Transparent,
                    contentColor = if (active)
                        MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text(title)
            }
        }
    }
}

@Composable
private fun PresetLineField(
    label: String,
    value: String,
    minLines: Int = 1,
    placeholder: String? = null,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = if (readOnly) ({ _ -> }) else onValueChange,
        readOnly = readOnly,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = if (minLines > 1) 12 else 1,
        shape = RoundedCornerShape(12.dp),
        colors = lineFieldColors(),
    )
}

@Composable
private fun PresetToggleField(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
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
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDropdownField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == value }?.second ?: value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            colors = lineFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (key, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onValueChange(key)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun parseInjectionTriggers(json: String): String {
    if (json.isBlank() || json == "[]") return ""
    return try {
        kotlinx.serialization.json.Json
            .decodeFromString<List<String>>(json)
            .joinToString(", ")
    } catch (_: Exception) {
        json
    }
}

private fun serializeInjectionTriggers(text: String): String {
    val triggers = text
        .split(",", "，", ";", "；")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    if (triggers.isEmpty()) return "[]"
    return kotlinx.serialization.json.Json.encodeToString(triggers)
}

private fun builtinPlaceholder(entry: PresetEntryEntity): String? {
    return when (entry.identifier.lowercase()) {
        "main" -> "{{main}}"
        "nsfw" -> "{{nsfw}}"
        "dialogueexamples" -> "{{dialogueExamples}}"
        "jailbreak" -> "{{jailbreak}}"
        "chathistory" -> "{{chatHistory}}"
        "worldinfoafter" -> "{{worldInfoAfter}}"
        "worldinfobefore" -> "{{worldInfoBefore}}"
        "enhancedefinitions" -> "{{enhanceDefinitions}}"
        "chardescription" -> "{{charDescription}}"
        "charpersonality" -> "{{charPersonality}}"
        "scenario" -> "{{scenario}}"
        "personadescription" -> "{{personaDescription}}"
        else -> null
    }
}

@Composable
private fun lineFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.outline,
    cursorColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.outline,
    unfocusedLabelColor = MaterialTheme.colorScheme.outline,
    focusedPlaceholderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
)
