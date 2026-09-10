package cn.mine.minestars.ui.pages.assistant.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import cn.mine.minestars.R
import cn.mine.minestars.data.db.dao.PresetDAO
import cn.mine.minestars.data.db.dao.PresetEntryDAO
import cn.mine.minestars.data.db.dao.RegexGroupDAO
import cn.mine.minestars.data.db.dao.UserPersonaDAO
import cn.mine.minestars.data.db.dao.WorldBookDAO
import cn.mine.minestars.data.import.DefaultPresetManager
import cn.mine.minestars.data.db.entity.PresetEntity
import cn.mine.minestars.data.db.entity.RegexGroupEntity
import cn.mine.minestars.data.db.entity.UserPersonaEntity
import cn.mine.minestars.data.db.entity.WorldBookEntity
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.theme.CustomColors
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowRight01

import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

class AssistantTavernBindingsVM(
    private val presetDao: PresetDAO,
    private val presetEntryDao: PresetEntryDAO,
    private val worldBookDao: WorldBookDAO,
    private val regexGroupDao: RegexGroupDAO,
    private val userPersonaDao: UserPersonaDAO,
) : ViewModel() {
    val presets = presetDao.getAllFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val worldBooks = worldBookDao.getAllFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val regexGroups = regexGroupDao.getAllFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val userPersonas = userPersonaDao.getAllFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            DefaultPresetManager.ensureDefaultPresetExists(presetDao, presetEntryDao)
        }
    }
}

@Composable
fun AssistantTavernBindingsPage(
    id: String,
    vm: AssistantTavernBindingsVM = koinViewModel(),
) {
    val detailVm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(id) })
    val assistant by detailVm.assistant.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val presets by vm.presets.collectAsStateWithLifecycle()
    val worldBooks by vm.worldBooks.collectAsStateWithLifecycle()
    val regexGroups by vm.regexGroups.collectAsStateWithLifecycle()
    val userPersonas by vm.userPersonas.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.assistant_tavern_title)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 预设选择
            item {
                PresetSection(
                    assistant = assistant,
                    presets = presets,
                    onSelectPreset = { presetId ->
                        detailVm.update(assistant.copy(presetId = presetId))
                    },
                )
            }

            // 用户设定选择
            item {
                UserPersonaSection(
                    assistant = assistant,
                    userPersonas = userPersonas,
                    onSelectUserPersona = { personaId ->
                        detailVm.update(assistant.copy(userPersonaId = personaId))
                    },
                )
            }

            // 世界书选择
            item {
                WorldBookSection(
                    assistant = assistant,
                    worldBooks = worldBooks,
                    onToggle = { id ->
                        detailVm.update(assistant.copy(worldBookIds = if (id != null) setOf(id) else emptySet()))
                    },
                )
            }

            // 正则组选择 — 统一滑动块 + 开关样式
            item {
                RegexGroupsSection(
                    assistant = assistant,
                    regexGroups = regexGroups,
                    onToggle = { groupId ->
                        detailVm.update(assistant.copy(regexGroupIds = groupId?.let { setOf(it) } ?: emptySet()))
                    },
                )
            }
        }
    }
}

// ── 预设 ──

@Composable
private fun PresetSection(
    assistant: Assistant,
    presets: List<PresetEntity>,
    onSelectPreset: (Uuid?) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Auto-select default if none selected
    LaunchedEffect(presets, assistant.presetId) {
        if (assistant.presetId == null && presets.isNotEmpty()) {
            val defaultPreset = presets.find { it.isDefault } ?: presets.first()
            onSelectPreset(runCatching { Uuid.parse(defaultPreset.id) }.getOrNull())
        }
    }

    Card(
        colors = CustomColors.cardColorsOnSurfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.assistant_tavern_preset), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (presets.isEmpty()) {
                Text(
                    stringResource(R.string.assistant_tavern_no_presets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                val selected = presets.find {
                    runCatching { Uuid.parse(it.id) }.getOrNull() == assistant.presetId
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSheet = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selected?.name ?: stringResource(R.string.assistant_tavern_select_preset),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(HugeIcons.ArrowRight01, null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
            ) {
                Text(
                    stringResource(R.string.assistant_tavern_select_preset),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                HorizontalDivider()
                presets.forEach { preset ->
                    val isSelected = runCatching { Uuid.parse(preset.id) }.getOrNull() == assistant.presetId
                    Card(
                        onClick = {
                            onSelectPreset(runCatching { Uuid.parse(preset.id) }.getOrNull())
                            scope.launch { sheetState.hide(); showSheet = false }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── 世界书 ──

@Composable
private fun WorldBookSection(
    assistant: Assistant,
    worldBooks: List<WorldBookEntity>,
    onToggle: (Uuid?) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enabled = assistant.worldBookIds.isNotEmpty()

    val selected = worldBooks.find {
        runCatching { Uuid.parse(it.id) }.getOrNull() in assistant.worldBookIds
    }

    Card(
        colors = CustomColors.cardColorsOnSurfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.assistant_tavern_world_book), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = { on ->
                        if (on) {
                            showSheet = true
                        } else {
                            onToggle(null)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                        checkedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        checkedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (worldBooks.isEmpty()) {
                Text(
                    stringResource(R.string.assistant_tavern_no_world_books),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { showSheet = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selected?.name ?: stringResource(R.string.assistant_tavern_no_bind),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled && selected != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                    )
                    if (enabled) {
                        Icon(HugeIcons.ArrowRight01, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    stringResource(R.string.assistant_tavern_select_world_book),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                HorizontalDivider()

                // stringResource(R.string.assistant_tavern_no_bind) option
                val noSelection = !enabled && selected == null
                Card(
                    onClick = {
                        onToggle(null)
                        scope.launch { sheetState.hide(); showSheet = false }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (noSelection) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.assistant_tavern_no_bind),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                worldBooks.sortedBy { it.name }.forEach { wb ->
                    val isSelected = runCatching { Uuid.parse(wb.id) }.getOrNull() in assistant.worldBookIds
                    Card(
                        onClick = {
                            onToggle(runCatching { Uuid.parse(wb.id) }.getOrNull())
                            scope.launch { sheetState.hide(); showSheet = false }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Text(
                            text = wb.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── 正则组 ──

@Composable
private fun RegexGroupsSection(
    assistant: Assistant,
    regexGroups: List<RegexGroupEntity>,
    onToggle: (Uuid?) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enabled = assistant.regexGroupIds.isNotEmpty()

    val selected = regexGroups.find {
        runCatching { Uuid.parse(it.id) }.getOrNull() in assistant.regexGroupIds
    }

    Card(
        colors = CustomColors.cardColorsOnSurfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.assistant_tavern_regex), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = { on ->
                        if (on) {
                            showSheet = true
                        } else {
                            onToggle(null)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                        checkedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        checkedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (regexGroups.isEmpty()) {
                Text(
                    stringResource(R.string.assistant_tavern_no_regex),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { showSheet = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selected?.name ?: stringResource(R.string.assistant_tavern_no_bind),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled && selected != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                    )
                    if (enabled) {
                        Icon(HugeIcons.ArrowRight01, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    stringResource(R.string.assistant_tavern_select_regex),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                HorizontalDivider()

                // stringResource(R.string.assistant_tavern_no_bind) option
                val noSelection = !enabled && selected == null
                Card(
                    onClick = {
                        onToggle(null)
                        scope.launch { sheetState.hide(); showSheet = false }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (noSelection) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.assistant_tavern_no_bind),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                regexGroups.sortedBy { it.name }.forEach { group ->
                    val isSelected = runCatching { Uuid.parse(group.id) }.getOrNull() in assistant.regexGroupIds
                    Card(
                        onClick = {
                            onToggle(runCatching { Uuid.parse(group.id) }.getOrNull())
                            scope.launch { sheetState.hide(); showSheet = false }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserPersonaSection(
    assistant: Assistant,
    userPersonas: List<UserPersonaEntity>,
    onSelectUserPersona: (Uuid?) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Card(
        colors = CustomColors.cardColorsOnSurfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.assistant_tavern_user_persona), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (userPersonas.isEmpty()) {
                Text(
                    stringResource(R.string.assistant_tavern_no_user_persona),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                val selected = userPersonas.find {
                    runCatching { Uuid.parse(it.id) }.getOrNull() == assistant.userPersonaId
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSheet = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selected?.name ?: stringResource(R.string.assistant_tavern_no_use),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(HugeIcons.ArrowRight01, null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
            ) {
                Text(
                    stringResource(R.string.assistant_tavern_select_user_persona),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                HorizontalDivider()

                // stringResource(R.string.assistant_tavern_no_use) option
                val noSelection = assistant.userPersonaId == null
                Card(
                    onClick = {
                        onSelectUserPersona(null)
                        scope.launch { sheetState.hide(); showSheet = false }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (noSelection) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.assistant_tavern_no_use),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                userPersonas.sortedBy { it.name }.forEach { persona ->
                    val isSelected = runCatching { Uuid.parse(persona.id) }.getOrNull() == assistant.userPersonaId
                    Card(
                        onClick = {
                            onSelectUserPersona(runCatching { Uuid.parse(persona.id) }.getOrNull())
                            scope.launch { sheetState.hide(); showSheet = false }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Text(
                            text = persona.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
