package cn.mine.minestars.ui.pages.setting

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.PencilEdit01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.MoreVertical
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import cn.mine.minestars.R
import cn.mine.minestars.Screen
import cn.mine.minestars.data.db.dao.SearchServiceDAO
import cn.mine.minestars.data.db.entity.SearchServiceEntity
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.components.ui.AutoAIIcon
import cn.mine.minestars.ui.components.ui.FormItem
import cn.mine.minestars.ui.components.ui.OutlinedNumberInput
import cn.mine.minestars.ui.components.ui.Tag
import cn.mine.minestars.ui.components.ui.TagType
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.JsonInstant
import cn.mine.minestars.utils.plus
import cn.mine.search.SearchCommonOptions
import cn.mine.search.SearchService
import cn.mine.search.SearchServiceOptions
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.reflect.full.primaryConstructor

@Composable
fun SettingSearchPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val aiSettings by vm.aiSettings.collectAsStateWithLifecycle()
    val searchServiceDAO: SearchServiceDAO = koinInject()
    val searchServiceEntities by searchServiceDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    val searchServices = remember(searchServiceEntities) {
        searchServiceEntities.map { it.toSearchServiceOptions() }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val nav = LocalNavController.current
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(stringResource(R.string.setting_page_search_title))
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true }
                    ) {
                        Icon(
                            imageVector = HugeIcons.Add01,
                            contentDescription = stringResource(R.string.setting_page_search_add_provider)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor
    ) {
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromIndex = from.index
            val toIndex = to.index

            if (fromIndex >= 0 && toIndex >= 0 && fromIndex < searchServices.size && toIndex < searchServices.size) {
                val newServices = searchServices.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                scope.launch {
                    searchServiceDAO.insertAll(newServices.map { it.toSearchServiceEntity() })
                }
            }
        }
        val haptic = LocalHapticFeedback.current

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = it + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            state = lazyListState
        ) {
            items(searchServices, key = { it.id }) { service ->
                ReorderableItem(
                    state = reorderableState,
                    key = service.id
                ) { isDragging ->
                    SearchProviderCard(
                        service = service,
                        onEdit = {
                            nav.navigate(Screen.SettingSearchDetail(service.id.toString()))
                        },
                        onDelete = {
                            if (searchServices.size > 1) {
                                scope.launch {
                                    searchServiceDAO.deleteById(service.id.toString())
                                }
                            }
                        },
                        canDelete = searchServices.size > 1,
                        modifier = Modifier
                            .scale(if (isDragging) 0.95f else 1f)
                            .animateItem()
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                },
                                onDragStopped = {
                                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                }
                            )
                    )
                }
            }

            item("common_options") {
                CommonOptions(
                    aiSettings = aiSettings,
                    onUpdate = { options ->
                        vm.updateAiSettings(
                            aiSettings.copy(searchCommonOptions = options)
                        )
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddProviderDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { options ->
                showAddDialog = false
                scope.launch {
                    searchServiceDAO.insert(options.toSearchServiceEntity())
                    lazyListState.animateScrollToItem(0)
                }
            }
        )
    }
}

private fun SearchServiceEntity.toSearchServiceOptions(): SearchServiceOptions {
    return JsonInstant.decodeFromString(this.config)
}

private fun SearchServiceOptions.toSearchServiceEntity(): SearchServiceEntity {
    return SearchServiceEntity(
        id = this.id.toString(),
        type = "",
        config = JsonInstant.encodeToString(this)
    )
}

@Composable
private fun AddProviderDialog(
    onDismiss: () -> Unit,
    onConfirm: (SearchServiceOptions) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(SearchServiceOptions.TYPES.keys.first())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.setting_page_search_add_provider))
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(SearchServiceOptions.TYPES.keys.toList()) { type ->
                    val name = SearchServiceOptions.TYPES[type] ?: "Unknown"
                    val isSelected = selectedType == type
                    Card(
                        onClick = { selectedType = type },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AutoAIIcon(
                                name = name,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val instance = selectedType.primaryConstructor!!.callBy(mapOf())
                    onConfirm(instance)
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SearchProviderCard(
    service: SearchServiceOptions,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = CustomColors.listItemColors.containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AutoAIIcon(
                name = service.displayName,
                modifier = Modifier.size(32.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = service.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                SearchAbilityTagLine(options = service)
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = HugeIcons.MoreVertical,
                    contentDescription = null
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(HugeIcons.PencilEdit01, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(HugeIcons.Delete01, contentDescription = null)
                        },
                        enabled = canDelete
                    )
                }
            }

        }
    }
}

@Composable
fun SearchAbilityTagLine(
    modifier: Modifier = Modifier,
    options: SearchServiceOptions
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Tag(
            type = TagType.DEFAULT,
        ) {
            Text(stringResource(R.string.search_ability_search))
        }
        if (SearchService.getService(options).scrapingParameters(options) != null) {
            Tag(
                type = TagType.DEFAULT,
            ) {
                Text(stringResource(R.string.search_ability_scrape))
            }
        }
    }
}

@Composable
private fun CommonOptions(
    aiSettings: cn.mine.minestars.data.datastore.AiSettings,
    onUpdate: (SearchCommonOptions) -> Unit
) {
    var commonOptions by remember(aiSettings.searchCommonOptions) {
        mutableStateOf(aiSettings.searchCommonOptions)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CustomColors.listItemColors.containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.setting_page_search_common_options),
                style = MaterialTheme.typography.titleMedium
            )

            FormItem(
                label = {
                    Text(stringResource(R.string.setting_page_search_result_size))
                }
            ) {
                OutlinedNumberInput(
                    value = commonOptions.resultSize,
                    onValueChange = {
                        commonOptions = commonOptions.copy(resultSize = it)
                        onUpdate(commonOptions)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
