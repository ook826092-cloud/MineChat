package cn.mine.minestars.ui.pages.setting

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Tick01
import me.rerere.hugeicons.stroke.StopCircle
import me.rerere.hugeicons.stroke.PencilEdit01
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Mic01
import me.rerere.hugeicons.stroke.Tools
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.VolumeHigh
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import cn.mine.asr.ASRProviderSetting
import cn.mine.minestars.R
import cn.mine.minestars.data.datastore.DEFAULT_SYSTEM_TTS_ID
import cn.mine.minestars.data.datastore.AiSettings
import cn.mine.minestars.data.db.dao.ASRProviderDAO
import cn.mine.minestars.data.db.dao.TTSProviderDAO
import cn.mine.minestars.data.db.entity.ASRProviderEntity
import cn.mine.minestars.data.db.entity.TTSProviderEntity
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.components.ui.AutoAIIcon
import cn.mine.minestars.ui.components.ui.Tag
import cn.mine.minestars.ui.components.ui.TagType
import cn.mine.minestars.ui.context.LocalTTSState
import cn.mine.minestars.ui.pages.setting.components.ASRProviderConfigure
import cn.mine.minestars.ui.pages.setting.components.TTSProviderConfigure
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.JsonInstant
import cn.mine.minestars.utils.plus
import cn.mine.tts.provider.TTSProviderSetting
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingSpeechPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val aiSettings by vm.aiSettings.collectAsStateWithLifecycle()
    val ttsProviderDAO: TTSProviderDAO = koinInject()
    val asrProviderDAO: ASRProviderDAO = koinInject()
    val ttsEntities by ttsProviderDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    val asrEntities by asrProviderDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    val ttsProviders = remember(ttsEntities) { ttsEntities.map { it.toTTSProviderSetting() } }
    val asrProviders = remember(asrEntities) { asrEntities.map { it.toASRProviderSetting() } }
    var editingTTSProvider by remember { mutableStateOf<TTSProviderSetting?>(null) }
    var editingASRProvider by remember { mutableStateOf<ASRProviderSetting?>(null) }
    var selectedPage by remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(R.string.speech_page_title))
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    if (selectedPage == 0) {
                        AddTTSProviderButton { provider ->
                            scope.launch {
                                ttsProviderDAO.insert(provider.toTTSProviderEntity())
                            }
                        }
                    } else {
                        AddASRProviderButton { provider ->
                            scope.launch {
                                asrProviderDAO.insert(provider.toASRProviderEntity())
                                vm.updateAiSettings(
                                    aiSettings.copy(selectedASRProviderId = provider.id)
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedPage == 0,
                    onClick = { selectedPage = 0 },
                    icon = { Icon(HugeIcons.VolumeHigh, contentDescription = null) },
                    label = { Text(stringResource(R.string.speech_tab_tts)) }
                )
                NavigationBarItem(
                    selected = selectedPage == 1,
                    onClick = { selectedPage = 1 },
                    icon = { Icon(HugeIcons.Mic01, contentDescription = null) },
                    label = { Text(stringResource(R.string.speech_tab_asr)) }
                )
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        when (selectedPage) {
            0 -> TTSProviderList(
                ttsProviders = ttsProviders,
                selectedTTSProviderId = aiSettings.selectedTTSProviderId,
                onSelect = { provider ->
                    vm.updateAiSettings(aiSettings.copy(selectedTTSProviderId = provider.id))
                },
                onEdit = { editingTTSProvider = it },
                onDelete = { provider ->
                    scope.launch {
                        ttsProviderDAO.deleteById(provider.id.toString())
                        if (aiSettings.selectedTTSProviderId == provider.id) {
                            vm.updateAiSettings(aiSettings.copy(selectedTTSProviderId = DEFAULT_SYSTEM_TTS_ID))
                        }
                    }
                },
                onReorder = { fromIndex, toIndex ->
                    val newProviders = ttsProviders.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                    scope.launch {
                        ttsProviderDAO.insertAll(newProviders.map { it.toTTSProviderEntity() })
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )

            1 -> ASRProviderList(
                asrProviders = asrProviders,
                selectedASRProviderId = aiSettings.selectedASRProviderId,
                onSelect = { provider ->
                    vm.updateAiSettings(aiSettings.copy(selectedASRProviderId = provider.id))
                },
                onEdit = { editingASRProvider = it },
                onDelete = { provider ->
                    scope.launch {
                        asrProviderDAO.deleteById(provider.id.toString())
                        val newSelectedId =
                            if (aiSettings.selectedASRProviderId == provider.id) {
                                asrProviders.firstOrNull()?.id
                            } else {
                                aiSettings.selectedASRProviderId
                            }
                        vm.updateAiSettings(aiSettings.copy(selectedASRProviderId = newSelectedId))
                    }
                },
                onReorder = { fromIndex, toIndex ->
                    val newProviders = asrProviders.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                    scope.launch {
                        asrProviderDAO.insertAll(newProviders.map { it.toASRProviderEntity() })
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    // Edit TTS Provider Bottom Sheet
    editingTTSProvider?.let { provider ->
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var currentProvider by remember(provider) { mutableStateOf(provider) }

        ModalBottomSheet(
            onDismissRequest = {
                editingTTSProvider = null
            },
            sheetState = bottomSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxHeight(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_tts_page_edit_provider),
                    style = MaterialTheme.typography.headlineSmall
                )

                TTSProviderConfigure(
                    setting = currentProvider,
                    onValueChange = { newState ->
                        currentProvider = newState
                    },
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            editingTTSProvider = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                ttsProviderDAO.insert(currentProvider.toTTSProviderEntity())
                            }
                            editingTTSProvider = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.chat_page_save))
                    }
                }
            }
        }
    }

    editingASRProvider?.let { provider ->
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var currentProvider by remember(provider) { mutableStateOf(provider) }

        ModalBottomSheet(
            onDismissRequest = {
                editingASRProvider = null
            },
            sheetState = bottomSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxHeight(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_asr_page_edit_provider),
                    style = MaterialTheme.typography.headlineSmall
                )

                ASRProviderConfigure(
                    setting = currentProvider,
                    onValueChange = { newState ->
                        currentProvider = newState
                    },
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            editingASRProvider = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                asrProviderDAO.insert(currentProvider.toASRProviderEntity())
                            }
                            editingASRProvider = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.chat_page_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun TTSProviderList(
    ttsProviders: List<TTSProviderSetting>,
    selectedTTSProviderId: kotlin.uuid.Uuid,
    onSelect: (TTSProviderSetting) -> Unit,
    onEdit: (TTSProviderSetting) -> Unit,
    onDelete: (TTSProviderSetting) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ttsProviders, key = { it.id }) { provider ->
            TTSProviderItem(
                provider = provider,
                isSelected = selectedTTSProviderId == provider.id,
                onSelect = { onSelect(provider) },
                onEdit = { onEdit(provider) },
                onDelete = { onDelete(provider) }
            )
        }
    }
}

@Composable
private fun ASRProviderList(
    asrProviders: List<ASRProviderSetting>,
    selectedASRProviderId: kotlin.uuid.Uuid?,
    onSelect: (ASRProviderSetting) -> Unit,
    onEdit: (ASRProviderSetting) -> Unit,
    onDelete: (ASRProviderSetting) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(asrProviders, key = { it.id }) { provider ->
            ASRProviderItem(
                provider = provider,
                isSelected = selectedASRProviderId == provider.id,
                onSelect = { onSelect(provider) },
                onEdit = { onEdit(provider) },
                onDelete = { onDelete(provider) }
            )
        }
    }
}

private fun TTSProviderEntity.toTTSProviderSetting(): TTSProviderSetting {
    return JsonInstant.decodeFromString(this.config)
}

private fun TTSProviderSetting.toTTSProviderEntity(): TTSProviderEntity {
    return TTSProviderEntity(
        id = this.id.toString(),
        name = this.name,
        type = this::class.simpleName ?: "Unknown",
        config = JsonInstant.encodeToString(this)
    )
}

private fun ASRProviderEntity.toASRProviderSetting(): ASRProviderSetting {
    return JsonInstant.decodeFromString(this.config)
}

private fun ASRProviderSetting.toASRProviderEntity(): ASRProviderEntity {
    return ASRProviderEntity(
        id = this.id.toString(),
        name = this.name,
        type = this::class.simpleName ?: "Unknown",
        config = JsonInstant.encodeToString(this)
    )
}

@Composable
private fun AddTTSProviderButton(onAdd: (TTSProviderSetting) -> Unit) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var currentProvider: TTSProviderSetting by remember { mutableStateOf(TTSProviderSetting.SystemTTS()) }

    IconButton(
        onClick = {
            currentProvider = TTSProviderSetting.SystemTTS()
            showBottomSheet = true
        }
    ) {
        Icon(HugeIcons.Add01, stringResource(R.string.setting_tts_page_add_provider_content_description))
    }

    if (showBottomSheet) {
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = bottomSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxHeight(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_tts_page_add_provider),
                    style = MaterialTheme.typography.headlineSmall
                )

                TTSProviderConfigure(
                    setting = currentProvider,
                    onValueChange = { newState ->
                        currentProvider = newState
                    },
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    TextButton(
                        onClick = {
                            onAdd(currentProvider)
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.setting_tts_page_add))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddASRProviderButton(onAdd: (ASRProviderSetting) -> Unit) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var currentProvider: ASRProviderSetting by remember { mutableStateOf(ASRProviderSetting.OpenAIRealtime()) }

    androidx.compose.foundation.layout.Box {
        IconButton(
            onClick = { showTypeMenu = true }
        ) {
            Icon(HugeIcons.Add01, stringResource(R.string.setting_asr_page_add_provider))
        }
        DropdownMenu(
            expanded = showTypeMenu,
            onDismissRequest = { showTypeMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("OpenAI Realtime") },
                onClick = {
                    currentProvider = ASRProviderSetting.OpenAIRealtime()
                    showTypeMenu = false
                    showBottomSheet = true
                }
            )
            DropdownMenuItem(
                text = { Text("DashScope") },
                onClick = {
                    currentProvider = ASRProviderSetting.DashScope()
                    showTypeMenu = false
                    showBottomSheet = true
                }
            )
            DropdownMenuItem(
                text = { Text("Volcengine") },
                onClick = {
                    currentProvider = ASRProviderSetting.Volcengine()
                    showTypeMenu = false
                    showBottomSheet = true
                }
            )
        }
    }

    if (showBottomSheet) {
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = bottomSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxHeight(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_asr_page_add_provider),
                    style = MaterialTheme.typography.headlineSmall
                )

                ASRProviderConfigure(
                    setting = currentProvider,
                    onValueChange = { newState ->
                        currentProvider = newState
                    },
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    TextButton(
                        onClick = {
                            onAdd(currentProvider)
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.setting_tts_page_add))
                    }
                }
            }
        }
    }
}

@Composable
private fun TTSProviderItem(
    provider: TTSProviderSetting,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    val tts = LocalTTSState.current
    val isSpeaking by tts.isSpeaking.collectAsState()
    val isAvailable by tts.isAvailable.collectAsState()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                CustomColors.listItemColors.containerColor
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AutoAIIcon(
                    name = provider.name.ifEmpty { stringResource(R.string.setting_tts_page_default_name) },
                    modifier = Modifier.size(32.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = provider.name.ifEmpty { stringResource(R.string.setting_tts_page_default_name) },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Text(
                        text = when (provider) {
                            is TTSProviderSetting.OpenAI -> stringResource(R.string.setting_tts_page_provider_openai)
                            is TTSProviderSetting.Gemini -> stringResource(R.string.setting_tts_page_provider_gemini)
                            is TTSProviderSetting.MiniMax -> "MiniMax"
                            is TTSProviderSetting.SystemTTS -> stringResource(R.string.setting_tts_page_provider_system)
                            is TTSProviderSetting.Qwen -> "Qwen"
                            is TTSProviderSetting.Groq -> "Groq"
                            is TTSProviderSetting.XAI -> "xAI"
                            is TTSProviderSetting.MiMo -> "MiMo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Tag(type = TagType.SUCCESS) {
                        Text(stringResource(R.string.setting_tts_page_selected))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isSelected && isAvailable) {
                    val testText = stringResource(R.string.setting_tts_page_test_text)
                    IconButton(
                        onClick = {
                            if (!isSpeaking) {
                                tts.speak(testText)
                            } else {
                                tts.stop()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) HugeIcons.StopCircle else HugeIcons.VolumeHigh,
                            contentDescription = if (isSpeaking) stringResource(R.string.stop) else stringResource(R.string.test_tts),
                            tint = if (isSpeaking) MaterialTheme.colorScheme.error else LocalContentColor.current
                        )
                    }
                }

                IconButton(
                    onClick = { showDropdownMenu = true }
                ) {
                    Icon(
                        imageVector = HugeIcons.Tools,
                        contentDescription = stringResource(R.string.setting_tts_page_more_options_content_description)
                    )
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                showDropdownMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(HugeIcons.PencilEdit01, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showDropdownMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(HugeIcons.Delete01, contentDescription = null)
                            },
                            enabled = provider.id != DEFAULT_SYSTEM_TTS_ID
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ASRProviderItem(
    provider: ASRProviderSetting,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                CustomColors.listItemColors.containerColor
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AutoAIIcon(
                    name = provider.name.ifEmpty { stringResource(R.string.setting_asr_page_default_name) },
                    modifier = Modifier.size(32.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = provider.name.ifEmpty { stringResource(R.string.setting_asr_page_default_name) },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Text(
                        text = when (provider) {
                            is ASRProviderSetting.OpenAIRealtime -> "OpenAI Realtime"
                            is ASRProviderSetting.DashScope -> "DashScope"
                            is ASRProviderSetting.Volcengine -> "Volcengine"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Tag(type = TagType.SUCCESS) {
                        Text(stringResource(R.string.setting_tts_page_selected))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { showDropdownMenu = true }
                ) {
                    Icon(
                        imageVector = HugeIcons.Tools,
                        contentDescription = stringResource(R.string.setting_tts_page_more_options_content_description)
                    )
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                showDropdownMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(HugeIcons.PencilEdit01, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showDropdownMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(HugeIcons.Delete01, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }
}
