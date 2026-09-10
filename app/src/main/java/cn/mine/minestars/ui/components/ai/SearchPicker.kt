package cn.mine.minestars.ui.components.ai

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import cn.mine.ai.provider.BuiltInTools
import cn.mine.ai.provider.Model
import cn.mine.ai.registry.ModelRegistry
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.GlobalSearch
import me.rerere.hugeicons.stroke.AiSearch02
import me.rerere.hugeicons.stroke.Search01
import me.rerere.hugeicons.stroke.Settings03
import cn.mine.minestars.R
import cn.mine.minestars.Screen
import cn.mine.minestars.data.datastore.AiSettings
import cn.mine.minestars.data.db.toProviderSettings
import cn.mine.minestars.data.db.toEntity
import cn.mine.minestars.data.db.dao.ProviderDAO
import cn.mine.minestars.data.db.dao.SearchServiceDAO
import cn.mine.minestars.data.db.entity.SearchServiceEntity
import cn.mine.minestars.ui.components.ui.AutoAIIcon
import cn.mine.minestars.ui.components.ui.ToggleSurface
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.context.Navigator
import cn.mine.minestars.ui.pages.setting.SearchAbilityTagLine
import cn.mine.minestars.utils.JsonInstant
import cn.mine.search.SearchServiceOptions
import org.koin.compose.koinInject

@Composable
fun SearchPickerButton(
    enableSearch: Boolean,
    searchServiceSelected: Int,
    modifier: Modifier = Modifier,
    onToggleSearch: (Boolean) -> Unit,
    onUpdateSearchService: (Int) -> Unit,
    model: Model?,
) {
    val searchServiceDAO: SearchServiceDAO = koinInject()
    val searchServiceEntities by searchServiceDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    val searchServices = remember(searchServiceEntities) {
        searchServiceEntities.map { it.toSearchServiceOptions() }
    }

    var showSearchPicker by remember { mutableStateOf(false) }
    val currentService = searchServices.getOrNull(searchServiceSelected)

    ToggleSurface(
        modifier = modifier,
        checked = enableSearch || model?.tools?.contains(BuiltInTools.Search) == true,
        onClick = {
            showSearchPicker = true
        }
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (model?.tools?.contains(BuiltInTools.Search) == true) {
                    Icon(
                        imageVector = HugeIcons.AiSearch02,
                        contentDescription = stringResource(R.string.use_web_search),
                    )
                } else if (enableSearch && currentService != null) {
                    AutoAIIcon(
                        name = currentService.displayName,
                        color = Color.Transparent
                    )
                } else {
                    Icon(
                        imageVector = HugeIcons.Search01,
                        contentDescription = stringResource(R.string.use_web_search),
                    )
                }
            }
        }
    }

    if (showSearchPicker) {
        ModalBottomSheet(
            onDismissRequest = { showSearchPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.search_picker_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                SearchPicker(
                    enableSearch = enableSearch,
                    searchServices = searchServices,
                    searchServiceSelected = searchServiceSelected,
                    onToggleSearch = onToggleSearch,
                    onUpdateSearchService = { index ->
                        onUpdateSearchService(index)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    model = model,
                    onDismiss = {
                        showSearchPicker = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchPicker(
    enableSearch: Boolean,
    searchServices: List<SearchServiceOptions>,
    searchServiceSelected: Int,
    model: Model?,
    modifier: Modifier = Modifier,
    onToggleSearch: (Boolean) -> Unit,
    onUpdateSearchService: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val navBackStack = LocalNavController.current

    // 模型内置搜索
    if (model != null && (ModelRegistry.GEMINI_SERIES.match(model.modelId) || model.modelId.contains("gpt-"))) {
        BuiltInSearchSetting(model = model)
    }

    // 如果没有开启内置搜索，显示搜索服务选择
    if (model?.tools?.contains(BuiltInTools.Search) != true) {
        AppSearchSettings(
            enableSearch = enableSearch,
            onDismiss = onDismiss,
            navBackStack = navBackStack,
            onToggleSearch = onToggleSearch,
            modifier = modifier,
            searchServices = searchServices,
            searchServiceSelected = searchServiceSelected,
            onUpdateSearchService = onUpdateSearchService
        )
    }
}

@Composable
private fun AppSearchSettings(
    enableSearch: Boolean,
    onDismiss: () -> Unit,
    navBackStack: Navigator,
    onToggleSearch: (Boolean) -> Unit,
    modifier: Modifier,
    searchServices: List<SearchServiceOptions>,
    searchServiceSelected: Int,
    onUpdateSearchService: (Int) -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(HugeIcons.GlobalSearch, null)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.use_web_search),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (enableSearch) {
                        stringResource(R.string.web_search_enabled)
                    } else {
                        stringResource(R.string.web_search_disabled)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }
            IconButton(
                onClick = {
                    onDismiss()
                    navBackStack.navigate(Screen.SettingSearch)
                }
            ) {
                Icon(HugeIcons.Settings03, null)
            }
            Switch(
                checked = enableSearch,
                onCheckedChange = onToggleSearch
            )
        }
    }

    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Adaptive(150.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(searchServices) { index, service ->
            val containerColor = animateColorAsState(
                if (searchServiceSelected == index) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            val textColor = animateColorAsState(
                if (searchServiceSelected == index) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = containerColor.value,
                    contentColor = textColor.value,
                ),
                onClick = {
                    onUpdateSearchService(index)
                },
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AutoAIIcon(
                        name = service.displayName,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = service.displayName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        SearchAbilityTagLine(
                            options = service,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuiltInSearchSetting(model: Model) {
    val providerDAO: ProviderDAO = koinInject()
    val scope = rememberCoroutineScope()
    Card {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(HugeIcons.GlobalSearch, null)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.built_in_search_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.built_in_search_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }

            Switch(
                checked = model.tools.contains(BuiltInTools.Search),
                onCheckedChange = { checked ->
                    scope.launch {
                        val allProviders = providerDAO.getAll().toProviderSettings()
                        allProviders.forEach { providerSetting ->
                            val updatedModels = providerSetting.models.map { m ->
                                if (m.id == model.id) {
                                    m.copy(
                                        tools = if (checked) m.tools + BuiltInTools.Search else m.tools - BuiltInTools.Search
                                    )
                                } else m
                            }
                            if (updatedModels != providerSetting.models) {
                                providerDAO.insert(
                                    providerSetting.copyProvider(models = updatedModels).toEntity(
                                        type = providerSetting::class.simpleName ?: "unknown",
                                        displayOrder = 0,
                                        builtIn = providerSetting.builtIn,
                                    )
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

private fun SearchServiceEntity.toSearchServiceOptions(): SearchServiceOptions {
    return JsonInstant.decodeFromString(this.config)
}
