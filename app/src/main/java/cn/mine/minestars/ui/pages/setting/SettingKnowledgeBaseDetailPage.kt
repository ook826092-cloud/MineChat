package cn.mine.minestars.ui.pages.setting

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Delete02
import me.rerere.hugeicons.stroke.Search01
import cn.mine.ai.provider.ModelType
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.db.dao.KnowledgeBaseDAO
import cn.mine.minestars.data.db.entity.KnowledgeBaseEntity
import cn.mine.minestars.R
import cn.mine.minestars.rag.EmbeddingService
import cn.mine.minestars.rag.KnowledgeBase
import cn.mine.minestars.ui.components.ai.ModelSelector
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.components.ui.FormItem
import cn.mine.minestars.ui.components.ui.BottomCreateToolbar
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.data.db.toProviderSettings
import cn.mine.minestars.data.db.dao.ProviderDAO
import cn.mine.minestars.utils.JsonInstant
import cn.mine.minestars.utils.plus
import kotlin.math.roundToInt
import org.koin.compose.koinInject

private fun KnowledgeBaseEntity.toKnowledgeBase(): KnowledgeBase {
    val files = runCatching {
        val configObj = JsonInstant.decodeFromString<kotlinx.serialization.json.JsonObject>(this.config)
        configObj["files"]?.jsonArray?.map { it.jsonPrimitive.content }
    }.getOrNull() ?: emptyList()
    return KnowledgeBase(
        id = kotlin.uuid.Uuid.parse(this.id),
        name = this.name,
        embeddingModelId = this.embeddingModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() },
        rerankModelId = this.rerankModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() },
        files = files,
        topK = this.topK,
        similarityThreshold = this.similarityThreshold,
        embeddingDimensions = this.embeddingDimensions,
    )
}

private fun KnowledgeBase.toEntity(): KnowledgeBaseEntity {
    val configObj = kotlinx.serialization.json.JsonObject(mapOf(
        "files" to kotlinx.serialization.json.JsonArray(this.files.map { kotlinx.serialization.json.JsonPrimitive(it) })
    ))
    return KnowledgeBaseEntity(
        id = this.id.toString(),
        name = this.name,
        embeddingModelId = this.embeddingModelId?.toString(),
        rerankModelId = this.rerankModelId?.toString(),
        topK = this.topK,
        similarityThreshold = this.similarityThreshold,
        embeddingDimensions = this.embeddingDimensions,
        config = JsonInstant.encodeToString(configObj),
    )
}

private fun getDisplayName(context: android.content.Context, uri: String): String {
    return try {
        val contentUri = Uri.parse(uri)
        context.contentResolver.query(contentUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } else null
        } ?: uri.substringAfterLast("/")
    } catch (_: Exception) {
        uri.substringAfterLast("/")
    }
}

@Composable
fun SettingKnowledgeBaseDetailPage(
    kbId: String,
    settingsStore: SettingsStore = koinInject(),
    embeddingService: EmbeddingService = koinInject(),
    providerDAO: ProviderDAO = koinInject(),
    knowledgeBaseDAO: KnowledgeBaseDAO = koinInject(),
) {
    val providers by providerDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    val providerSettings = remember(providers) { providers.toProviderSettings() }
    val knowledgeBaseEntities by knowledgeBaseDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    val knowledgeBases = remember(knowledgeBaseEntities) {
        knowledgeBaseEntities.map { it.toKnowledgeBase() }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val kb = remember(knowledgeBases, kbId) {
        knowledgeBases.find { it.id.toString() == kbId }
    }

    var name by remember(kb) { mutableStateOf(kb?.name ?: "") }
    var embeddingModelId by remember(kb) { mutableStateOf(kb?.embeddingModelId) }
    var rerankModelId by remember(kb) { mutableStateOf(kb?.rerankModelId) }
    var topK by remember(kb) { mutableIntStateOf(kb?.topK ?: 5) }
    var threshold by remember(kb) { mutableFloatStateOf(kb?.similarityThreshold ?: 0.5f) }
    var dimensions by remember(kb) { mutableStateOf(kb?.embeddingDimensions?.toString() ?: "") }

    var indexedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var indexingFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearchDialog by remember { mutableStateOf(false) }

    fun saveKb(current: KnowledgeBase, update: KnowledgeBase.() -> KnowledgeBase) {
        val newKb = current.update()
        scope.launch {
            knowledgeBaseDAO.insert(newKb.toEntity())
        }
    }

    fun indexFile(uri: String) {
        val currentKb = kb ?: return
        if (indexingFiles.contains(uri)) return
        indexingFiles = indexingFiles + uri
        scope.launch {
            try {
                embeddingService.embedFile(currentKb, uri)
                indexedFiles = indexedFiles + uri
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: context.getString(R.string.kb_detail_index_failed), Toast.LENGTH_SHORT).show()
            } finally {
                indexingFiles = indexingFiles - uri
            }
        }
    }

    LaunchedEffect(kbId) {
        indexedFiles = embeddingService.getIndexedFiles(kbId).toSet()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val kb = kb ?: return@rememberLauncherForActivityResult
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val newUris = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            uri.toString()
        }
        val newFiles = (kb.files + newUris).distinct()
        saveKb(kb) { copy(files = newFiles) }

        // Auto-index new files
        newUris.forEach { uri ->
            indexingFiles = indexingFiles + uri
            scope.launch {
                try {
                    embeddingService.embedFile(kb, uri)
                    indexedFiles = indexedFiles + uri
                } catch (e: Exception) {
                    Toast.makeText(context, e.message ?: context.getString(R.string.kb_detail_index_failed), Toast.LENGTH_SHORT).show()
                } finally {
                    indexingFiles = indexingFiles - uri
                }
            }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(name.ifBlank { stringResource(R.string.kb_detail_title_fallback) }) },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(HugeIcons.Search01, contentDescription = stringResource(R.string.kb_detail_search_test_cd))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        if (kb == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.kb_detail_not_found), style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Name - always visible at top
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        saveKb(kb) { copy(name = it) }
                    },
                    label = { Text(stringResource(R.string.kb_detail_name_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                )

                // Tabs
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.kb_detail_tab_files)) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.kb_detail_tab_config)) },
                    )
                }

                // Tab content
                when (selectedTab) {
                    0 -> FilesTab(
                        files = kb.files,
                        indexedFiles = indexedFiles,
                        indexingFiles = indexingFiles,
                        onIndex = { indexFile(it) },
                        onDelete = { uri ->
                            saveKb(kb) { copy(files = files.filter { it != uri }) }
                            scope.launch {
                                embeddingService.removeFile(kb.id.toString(), uri)
                                indexedFiles = indexedFiles - uri
                            }
                        },
                    )
                    1 -> ConfigTab(
                        embeddingModelId = embeddingModelId,
                        rerankModelId = rerankModelId,
                        topK = topK,
                        threshold = threshold,
                        dimensions = dimensions,
                        providers = providerSettings,
                        onEmbeddingModelChange = {
                            embeddingModelId = it.id
                            saveKb(kb) { copy(embeddingModelId = it.id) }
                        },
                        onRerankModelChange = {
                            rerankModelId = it.id
                            saveKb(kb) { copy(rerankModelId = it.id) }
                        },
                        onTopKChange = {
                            topK = it
                            saveKb(kb) { copy(topK = it) }
                        },
                        onThresholdChange = {
                            threshold = it
                            saveKb(kb) { copy(similarityThreshold = threshold) }
                        },
                        onDimensionsChange = { value ->
                            dimensions = value
                            saveKb(kb) { copy(embeddingDimensions = value.toIntOrNull()) }
                        },
                    )
                }
            }

            // 仅在文件 Tab 底部显示添加按钮
            if (selectedTab == 0) {
                BottomCreateToolbar(
                    text = stringResource(R.string.kb_detail_add_file),
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }

    if (kb != null && showSearchDialog) {
        KbSearchTestDialog(
            kb = kb,
            embeddingService = embeddingService,
            getDisplayName = { uri -> getDisplayName(context, uri) },
            onDismiss = { showSearchDialog = false },
        )
    }
}

@Composable
private fun KbSearchTestDialog(
    kb: KnowledgeBase,
    embeddingService: EmbeddingService,
    getDisplayName: (String) -> String,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<EmbeddingService.SearchResult>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            colors = CustomColors.cardColorsOnSurfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.kb_detail_search_test_title),
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.kb_detail_search_query_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.kb_detail_threshold_format, "%.2f".format(kb.similarityThreshold), kb.topK),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            if (query.isBlank()) return@Button
                            isSearching = true
                            error = null
                            results = null
                            scope.launch {
                                try {
                                    results = embeddingService.searchSimilar(
                                        kb = kb,
                                        query = query,
                                        topK = kb.topK,
                                        threshold = 0f,
                                    )
                                } catch (e: Exception) {
                                    error = e.message ?: context.getString(R.string.kb_detail_search_failed)
                                    results = emptyList()
                                } finally {
                                    isSearching = false
                                }
                            }
                        },
                        enabled = !isSearching,
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(if (isSearching) stringResource(R.string.kb_detail_searching) else stringResource(R.string.kb_detail_search))
                    }
                }

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.kb_detail_error_prefix, error!!),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (results != null) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.kb_detail_search_results, results!!.size),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    if (results!!.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.kb_detail_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        results!!.forEachIndexed { index, result ->
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CustomColors.cardColors,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        val passesThreshold = result.similarity >= kb.similarityThreshold
                                        Text(
                                            text = stringResource(R.string.kb_detail_similarity_format, "%.4f".format(result.similarity), if (passesThreshold) stringResource(R.string.kb_detail_above_threshold) else stringResource(R.string.kb_detail_below_threshold)),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (passesThreshold) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.kb_detail_source_label, getDisplayName(result.fileUri)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = result.chunkText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.kb_detail_close))
                }
            }
        }
    }
}

@Composable
private fun FilesTab(
    files: List<String>,
    indexedFiles: Set<String>,
    indexingFiles: Set<String>,
    onIndex: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.kb_detail_no_files),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(stringResource(R.string.kb_detail_file_list), style = MaterialTheme.typography.titleMedium)
            }

            items(files.size) { index ->
                val fileUri = files[index]
                val fileName = getDisplayName(
                    androidx.compose.ui.platform.LocalContext.current,
                    fileUri,
                )
                val isIndexed = indexedFiles.contains(fileUri)
                val isIndexing = indexingFiles.contains(fileUri)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName.ifBlank { stringResource(R.string.kb_detail_file_fallback) },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = when {
                                isIndexing -> stringResource(R.string.kb_detail_indexing)
                                isIndexed -> stringResource(R.string.kb_detail_indexed)
                                else -> stringResource(R.string.kb_detail_not_indexed)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isIndexed) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (!isIndexing) {
                        OutlinedButton(onClick = { onIndex(fileUri) }) {
                            Text(if (isIndexed) stringResource(R.string.kb_detail_reindex) else stringResource(R.string.kb_detail_index))
                        }
                    }

                    IconButton(onClick = { onDelete(fileUri) }) {
                        Icon(HugeIcons.Delete02, stringResource(R.string.kb_detail_delete_cd))
                    }
                }
                if (isIndexing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

    }
}

@Composable
private fun ConfigTab(
    embeddingModelId: kotlin.uuid.Uuid?,
    rerankModelId: kotlin.uuid.Uuid?,
    topK: Int,
    threshold: Float,
    dimensions: String,
    providers: List<cn.mine.ai.provider.ProviderSetting>,
    onEmbeddingModelChange: (cn.mine.ai.provider.Model) -> Unit,
    onRerankModelChange: (cn.mine.ai.provider.Model) -> Unit,
    onTopKChange: (Int) -> Unit,
    onThresholdChange: (Float) -> Unit,
    onDimensionsChange: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(colors = CustomColors.cardColorsOnSurfaceContainer) {
                FormItem(
                    modifier = Modifier.padding(8.dp),
                    label = { Text(stringResource(R.string.kb_detail_embedding_model)) },
                    description = { Text(stringResource(R.string.kb_detail_embedding_model_desc)) },
                    content = {
                        ModelSelector(
                            modelId = embeddingModelId,
                            providers = providers,
                            type = ModelType.EMBEDDING,
                            onSelect = onEmbeddingModelChange,
                        )
                    }
                )
            }
        }

        item {
            Card(colors = CustomColors.cardColorsOnSurfaceContainer) {
                FormItem(
                    modifier = Modifier.padding(8.dp),
                    label = { Text(stringResource(R.string.kb_detail_rerank_model)) },
                    description = { Text(stringResource(R.string.kb_detail_rerank_model_desc)) },
                    content = {
                        ModelSelector(
                            modelId = rerankModelId,
                            providers = providers,
                            type = ModelType.RERANK,
                            onSelect = onRerankModelChange,
                        )
                    }
                )
            }
        }

        item {
            Card(colors = CustomColors.cardColorsOnSurfaceContainer) {
                FormItem(
                    modifier = Modifier.padding(8.dp),
                    label = { Text(stringResource(R.string.kb_detail_top_k)) },
                    description = { Text(stringResource(R.string.kb_detail_top_k_desc)) },
                ) {
                    Slider(
                        value = topK.toFloat(),
                        onValueChange = { onTopKChange(it.roundToInt()) },
                        valueRange = 1f..20f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = topK.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                    )
                }
            }
        }

        item {
            Card(colors = CustomColors.cardColorsOnSurfaceContainer) {
                FormItem(
                    modifier = Modifier.padding(8.dp),
                    label = { Text(stringResource(R.string.kb_detail_similarity_threshold)) },
                    description = { Text(stringResource(R.string.kb_detail_similarity_threshold_desc)) },
                ) {
                    Slider(
                        value = threshold,
                        onValueChange = { onThresholdChange((it * 100).roundToInt() / 100f) },
                        valueRange = 0f..1f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "%.2f".format(threshold),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                    )
                }
            }
        }

        item {
            Card(colors = CustomColors.cardColorsOnSurfaceContainer) {
                FormItem(
                    modifier = Modifier.padding(8.dp),
                    label = { Text(stringResource(R.string.kb_detail_vector_dimension)) },
                    description = { Text(stringResource(R.string.kb_detail_vector_dimension_desc)) },
                    content = {
                        OutlinedTextField(
                            value = dimensions,
                            onValueChange = onDimensionsChange,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.kb_detail_use_model_default)) },
                        )
                    }
                )
            }
        }
    }
}
