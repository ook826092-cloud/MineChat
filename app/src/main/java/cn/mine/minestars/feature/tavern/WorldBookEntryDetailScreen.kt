package cn.mine.minestars.feature.tavern

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import cn.mine.minestars.core.tavern.worldbook.WorldBookEntry
import cn.mine.minestars.ui.context.LocalNavController
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowLeft01
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.plus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBookEntryDetailPage(
    bookId: String,
    entryIndex: Int,
    assistantId: String? = null,
) {
    val vm: WorldBookDetailVM = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
    )
    val navController = LocalNavController.current
    val entries by vm.entries.collectAsStateWithLifecycle()
    val entry = entries.getOrNull(entryIndex)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Local state synced from VM entry
    var title by remember(entryIndex, entry) { mutableStateOf(entry?.title.orEmpty()) }
    var content by remember(entryIndex, entry) { mutableStateOf(entry?.content.orEmpty()) }
    var enabled by remember(entryIndex, entry) { mutableStateOf(entry?.enabled ?: true) }
    var constant by remember(entryIndex, entry) { mutableStateOf(entry?.constant ?: false) }
    var forceActivate by remember(entryIndex, entry) { mutableStateOf(entry?.forceActivate ?: false) }
    var forceDisable by remember(entryIndex, entry) { mutableStateOf(entry?.forceDisable ?: false) }

    var keysText by remember(entryIndex, entry) { mutableStateOf(entry?.keys?.joinToString("\n") ?: "") }
    var secondaryKeysText by remember(entryIndex, entry) { mutableStateOf(entry?.secondaryKeys?.joinToString("\n") ?: "") }
    var selective by remember(entryIndex, entry) { mutableStateOf(entry?.selective ?: true) }
    var selectiveLogic by remember(entryIndex, entry) { mutableStateOf(entry?.selectiveLogic ?: 0) }
    var useRegex by remember(entryIndex, entry) { mutableStateOf(entry?.useRegex ?: false) }
    var caseSensitiveStr by remember(entryIndex, entry) { mutableStateOf(tristateToString(entry?.caseSensitive)) }
    var matchWholeWordsStr by remember(entryIndex, entry) { mutableStateOf(tristateToString(entry?.matchWholeWords)) }
    var useGroupScoringStr by remember(entryIndex, entry) { mutableStateOf(tristateToString(entry?.useGroupScoring)) }

    var position by remember(entryIndex, entry) { mutableStateOf(entry?.position ?: 0) }
    var depth by remember(entryIndex, entry) { mutableStateOf(entry?.depth?.toString().orEmpty()) }
    var order by remember(entryIndex, entry) { mutableStateOf(entry?.order.toString()) }
    var role by remember(entryIndex, entry) { mutableStateOf(entry?.role ?: "system") }
    var outletName by remember(entryIndex, entry) { mutableStateOf(entry?.outletName.orEmpty()) }

    var probability by remember(entryIndex, entry) { mutableStateOf(entry?.probability?.toString().orEmpty()) }
    var useProbability by remember(entryIndex, entry) { mutableStateOf(entry?.useProbability ?: true) }
    var sticky by remember(entryIndex, entry) { mutableStateOf(entry?.sticky?.toString().orEmpty()) }
    var cooldown by remember(entryIndex, entry) { mutableStateOf(entry?.cooldown?.toString().orEmpty()) }
    var delay by remember(entryIndex, entry) { mutableStateOf(entry?.delay?.toString().orEmpty()) }
    var scanDepth by remember(entryIndex, entry) { mutableStateOf(entry?.scanDepth?.toString().orEmpty()) }

    var group by remember(entryIndex, entry) { mutableStateOf(entry?.group ?: "") }
    var groupOverride by remember(entryIndex, entry) { mutableStateOf(entry?.groupOverride ?: false) }
    var groupWeight by remember(entryIndex, entry) { mutableStateOf(entry?.groupWeight?.toString() ?: "100") }
    var ignoreBudget by remember(entryIndex, entry) { mutableStateOf(entry?.ignoreBudget ?: false) }
    var excludeRecursion by remember(entryIndex, entry) { mutableStateOf(entry?.excludeRecursion ?: false) }
    var preventRecursion by remember(entryIndex, entry) { mutableStateOf(entry?.preventRecursion ?: false) }
    var delayUntilRecursion by remember(entryIndex, entry) { mutableStateOf(entry?.delayUntilRecursion ?: false) }
    var automationId by remember(entryIndex, entry) { mutableStateOf(entry?.automationId.orEmpty()) }
    var triggersText by remember(entryIndex, entry) { mutableStateOf(entry?.triggers?.joinToString("\n") ?: "") }

    var matchPersona by remember(entryIndex, entry) { mutableStateOf(entry?.matchPersonaDescription ?: false) }
    var matchCharDesc by remember(entryIndex, entry) { mutableStateOf(entry?.matchCharacterDescription ?: false) }
    var matchCharPers by remember(entryIndex, entry) { mutableStateOf(entry?.matchCharacterPersonality ?: false) }
    var matchDepthPrompt by remember(entryIndex, entry) { mutableStateOf(entry?.matchCharacterDepthPrompt ?: false) }
    var matchScenario by remember(entryIndex, entry) { mutableStateOf(entry?.matchScenario ?: false) }
    var matchCreatorNotes by remember(entryIndex, entry) { mutableStateOf(entry?.matchCreatorNotes ?: false) }

    var characterFilterText by remember(entryIndex, entry) { mutableStateOf(entry?.characterFilterNames?.joinToString("\n") ?: "") }
    var characterFilterTagsText by remember(entryIndex, entry) { mutableStateOf(entry?.characterFilterTags?.joinToString("\n") ?: "") }
    var characterFilterExclude by remember(entryIndex, entry) { mutableStateOf(entry?.characterFilterExclude ?: false) }

    val saveEntryAndGoBack: () -> Unit = {
        if (entry != null) {
            val updated = buildEntryFromState(
                entry, title, content, enabled, constant,
                forceActivate, forceDisable, keysText, secondaryKeysText,
                selective, selectiveLogic, useRegex, caseSensitiveStr,
                matchWholeWordsStr, useGroupScoringStr, position, depth,
                order, role, outletName, probability, useProbability,
                sticky, cooldown, delay, scanDepth, group, groupOverride,
                groupWeight, ignoreBudget, excludeRecursion, preventRecursion,
                delayUntilRecursion, automationId, triggersText, matchPersona,
                matchCharDesc, matchCharPers, matchDepthPrompt, matchScenario,
                matchCreatorNotes, characterFilterText, characterFilterTagsText,
                characterFilterExclude,
            )
            vm.updateEntry(entryIndex, updated)
        }
        navController.popBackStack()
    }

    BackHandler {
        saveEntryAndGoBack()
    }

    LaunchedEffect(bookId, assistantId) {
        if (entries.isEmpty()) {
            if (assistantId != null) {
                vm.loadAssistantBook(assistantId)
            } else {
                vm.loadBook(bookId)
            }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title.ifBlank { "条目详情" }, maxLines = 1) },
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
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("条目不存在", color = MaterialTheme.colorScheme.outline)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding(),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("内容") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("关键字") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("激活") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("高级") })
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item { SectionTitle2("注入内容") }
                        item {
                            EntryLineField("标题", title) { title = it }
                        }
                        item {
                            EntryLineField("内容", content, minLines = 6) { content = it }
                        }
                        item {
                            EntryToggle("始终激活 (Constant)", constant) { constant = it }
                        }
                    }
                    1 -> LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item { SectionTitle2("主要关键字（每行一个）") }
                        item {
                            EntryLineField("主要关键字", keysText, minLines = 3) { keysText = it }
                        }
                        item { SectionTitle2("次要关键字（每行一个）") }
                        item {
                            EntryLineField("次要关键字", secondaryKeysText, minLines = 2) { secondaryKeysText = it }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EntryToggle("选择性", selective, modifier = Modifier.weight(1f)) { selective = it }
                                EntryToggle("使用正则", useRegex, modifier = Modifier.weight(1f)) { useRegex = it }
                            }
                        }
                        item {
                            EntryDropdown(
                                label = "关键字逻辑",
                                value = selectiveLogic,
                                options = listOf(0 to "AND ANY", 1 to "NOT ALL", 2 to "NOT ANY", 3 to "AND ALL"),
                            ) { selectiveLogic = it }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EntryTristateDropdown("大小写敏感", caseSensitiveStr, modifier = Modifier.weight(1f)) {
                                    caseSensitiveStr = it                        }
                                EntryTristateDropdown("匹配完整单词", matchWholeWordsStr, modifier = Modifier.weight(1f)) {
                                    matchWholeWordsStr = it                        }
                            }
                        }
                        item {
                            EntryTristateDropdown("组评分", useGroupScoringStr, modifier = Modifier.fillMaxWidth()) {
                                useGroupScoringStr = it                    }
                        }
                    }
                    2 -> LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            EntryDropdown(
                                label = "注入位置",
                                value = position,
                                options = listOf(
                                    0 to "角色定义前",
                                    1 to "角色定义后",
                                    2 to "AN 前",
                                    3 to "AN 后",
                                    4 to "指定深度",
                                    5 to "EM 前",
                                    6 to "EM 后",
                                    7 to "出口",
                                ),
                            ) { position = it }
                        }
                        if (position == 4) {
                            item {
                                EntryLineField("深度", depth, keyboardType = KeyboardType.Number) { depth = it }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EntryLineField("顺序", order, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { order = it }
                                EntryLineField("概率 %", probability, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { probability = it }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EntryToggle("使用概率", useProbability, modifier = Modifier.weight(1f)) { useProbability = it }
                                EntryToggle("忽略预算", ignoreBudget, modifier = Modifier.weight(1f)) { ignoreBudget = it }
                            }
                        }
                        item {
                            EntryDropdown(
                                label = "注入角色",
                                value = role,
                                options = listOf("system" to "系统", "user" to "用户", "assistant" to "助手"),
                            ) { role = it }
                        }
                        item {
                            EntryLineField("出口名称", outletName) { outletName = it }
                        }
                        item { SectionTitle2("黏着/冷却/延迟") }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EntryLineField("黏着", sticky, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { sticky = it }
                                EntryLineField("冷却", cooldown, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { cooldown = it }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EntryLineField("延迟", delay, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { delay = it }
                                EntryLineField("扫描深度", scanDepth, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { scanDepth = it }
                            }
                        }
                    }
                    3 -> LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item { SectionTitle2("包含组") }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EntryLineField("组名", group, modifier = Modifier.weight(1f)) { group = it }
                                EntryLineField("组权重", groupWeight, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { groupWeight = it }
                            }
                        }
                        item {
                            EntryToggle("组内优先", groupOverride) { groupOverride = it }
                        }
                        item { SectionTitle2("递归") }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EntryToggle("非递归", excludeRecursion, modifier = Modifier.weight(1f)) { excludeRecursion = it }
                                EntryToggle("阻止递归", preventRecursion, modifier = Modifier.weight(1f)) { preventRecursion = it }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EntryToggle("延迟递归", delayUntilRecursion, modifier = Modifier.weight(1f)) { delayUntilRecursion = it }
                                EntryToggle("强制激活", forceActivate, modifier = Modifier.weight(1f)) { forceActivate = it }
                            }
                        }
                        item {
                            EntryToggle("强制禁用", forceDisable) { forceDisable = it }
                        }
                        item { SectionTitle2("额外匹配来源") }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "角色描述" to matchCharDesc,
                                    "角色性格" to matchCharPers,
                                    "场景" to matchScenario,
                                    "人设描述" to matchPersona,
                                    "角色深度提示" to matchDepthPrompt,
                                    "创作者笔记" to matchCreatorNotes,
                                ).chunked(2).forEach { chunk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        chunk.forEach { (label, checked) ->
                                            ExtraMatchChip(
                                                text = label,
                                                selected = checked,
                                                onClick = {
                                                    when (label) {
                                                        "角色描述" -> matchCharDesc = !matchCharDesc
                                                        "角色性格" -> matchCharPers = !matchCharPers
                                                        "场景" -> matchScenario = !matchScenario
                                                        "人设描述" -> matchPersona = !matchPersona
                                                        "角色深度提示" -> matchDepthPrompt = !matchDepthPrompt
                                                        "创作者笔记" -> matchCreatorNotes = !matchCreatorNotes
                                                    }
                                                },
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
                        item { SectionTitle2("生成触发器（每行一个）") }
                        item {
                            EntryLineField("触发器", triggersText, minLines = 2) { triggersText = it }
                        }
                        item { SectionTitle2("自动化和角色过滤") }
                        item {
                            EntryLineField("自动化 ID", automationId) { automationId = it }
                        }
                        item {
                            EntryLineField("角色过滤（每行一个名称）", characterFilterText, minLines = 2) { characterFilterText = it }
                        }
                        item {
                            EntryLineField("角色标签过滤（每行一个）", characterFilterTagsText, minLines = 2) { characterFilterTagsText = it }
                        }
                        item {
                            EntryToggle("排除模式（角色过滤为排除）", characterFilterExclude) { characterFilterExclude = it }
                        }
                    }
                }
            }
        }
    }

}

private fun buildEntryFromState(
    base: WorldBookEntry, title: String, content: String, enabled: Boolean,
    constant: Boolean, forceActivate: Boolean, forceDisable: Boolean,
    keysText: String, secondaryKeysText: String, selective: Boolean,
    selectiveLogic: Int, useRegex: Boolean, caseSensitiveStr: String,
    matchWholeWordsStr: String, useGroupScoringStr: String, position: Int,
    depth: String, order: String, role: String, outletName: String,
    probability: String, useProbability: Boolean, sticky: String,
    cooldown: String, delay: String, scanDepth: String, group: String,
    groupOverride: Boolean, groupWeight: String, ignoreBudget: Boolean,
    excludeRecursion: Boolean, preventRecursion: Boolean, delayUntilRecursion: Boolean,
    automationId: String, triggersText: String, matchPersona: Boolean,
    matchCharDesc: Boolean, matchCharPers: Boolean, matchDepthPrompt: Boolean,
    matchScenario: Boolean, matchCreatorNotes: Boolean, characterFilterText: String,
    characterFilterTagsText: String, characterFilterExclude: Boolean,
): WorldBookEntry {
    fun parseLines(text: String) = text.split(Regex("[,，\n]")).map { it.trim() }.filter { it.isNotBlank() }.distinct()
    fun parseNullableInt(text: String) = text.trim().toIntOrNull()
    fun parseTristate(s: String): Boolean? = when (s) {
        "true" -> true; "false" -> false; else -> null
    }
    return base.copy(
        title = title.trim().ifBlank { null },
        content = content,
        enabled = enabled,
        constant = constant,
        forceActivate = forceActivate,
        forceDisable = forceDisable,
        keys = parseLines(keysText),
        secondaryKeys = parseLines(secondaryKeysText),
        selective = selective,
        selectiveLogic = selectiveLogic,
        useRegex = useRegex,
        caseSensitive = parseTristate(caseSensitiveStr),
        matchWholeWords = parseTristate(matchWholeWordsStr),
        useGroupScoring = parseTristate(useGroupScoringStr),
        position = position,
        depth = parseNullableInt(depth),
        order = parseNullableInt(order) ?: base.order,
        role = role,
        outletName = outletName.trim().ifBlank { null },
        probability = parseNullableInt(probability),
        useProbability = useProbability,
        sticky = parseNullableInt(sticky),
        cooldown = parseNullableInt(cooldown),
        delay = parseNullableInt(delay),
        scanDepth = parseNullableInt(scanDepth),
        group = group.trim(),
        groupOverride = groupOverride,
        groupWeight = parseNullableInt(groupWeight) ?: 100,
        ignoreBudget = ignoreBudget,
        excludeRecursion = excludeRecursion,
        preventRecursion = preventRecursion,
        delayUntilRecursion = delayUntilRecursion,
        automationId = automationId.trim().ifBlank { null },
        triggers = parseLines(triggersText),
        matchPersonaDescription = matchPersona,
        matchCharacterDescription = matchCharDesc,
        matchCharacterPersonality = matchCharPers,
        matchCharacterDepthPrompt = matchDepthPrompt,
        matchScenario = matchScenario,
        matchCreatorNotes = matchCreatorNotes,
        characterFilterNames = parseLines(characterFilterText),
        characterFilterTags = parseLines(characterFilterTagsText),
        characterFilterExclude = characterFilterExclude,
    )
}

private fun tristateToString(v: Boolean?): String = when (v) {
    true -> "true"; false -> "false"; null -> ""
}

@Composable
private fun SectionTitle2(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
private fun EntryLineField(
    label: String,
    value: String,
    minLines: Int = 1,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = if (minLines > 1) 12 else 1,
        shape = RoundedCornerShape(12.dp),
        colors = entryFieldColors(),
    )
}

@Composable
private fun EntryToggle(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = entrySwitchColors())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDropdown(
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
            colors = entryFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onValueChange(key); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == value }?.second ?: value
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            colors = entryFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onValueChange(key); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryTristateDropdown(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val display = when (value) { "true" -> "是"; "false" -> "否"; else -> "默认" }
    val options = listOf("" to "默认", "true" to "是", "false" to "否")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = entryFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onValueChange(key); expanded = false })
            }
        }
    }
}

@Composable
private fun entryFieldColors() = OutlinedTextFieldDefaults.colors(
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
private fun entrySwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onSurface,
    checkedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    checkedBorderColor = MaterialTheme.colorScheme.outline,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
)

@Composable
private fun ExtraMatchChip(
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
