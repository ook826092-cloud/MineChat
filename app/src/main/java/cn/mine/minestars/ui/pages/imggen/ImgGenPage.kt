package cn.mine.minestars.ui.pages.imggen

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.dokar.sonner.ToastType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cn.mine.ai.provider.ModelType
import cn.mine.ai.provider.ParamType
import cn.mine.ai.provider.ProviderSetting
import cn.mine.common.android.appTempFolder
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.ArrowLeft02
import me.rerere.hugeicons.stroke.ArrowUp02
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Colors
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.FloppyDisk
import me.rerere.hugeicons.stroke.Image03
import me.rerere.hugeicons.stroke.Tools
import cn.mine.minestars.R
import cn.mine.minestars.ui.components.ui.CardGroup
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.db.dao.ModelSelectionDAO
import cn.mine.minestars.data.db.entity.ModelSelectionEntity
import cn.mine.minestars.data.db.dao.ProviderDAO
import cn.mine.minestars.data.db.toProviderSettings
import cn.mine.minestars.data.files.FileUtils
import cn.mine.minestars.data.files.FilesManager
import cn.mine.minestars.ui.components.ai.ModelSelector
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.components.ui.ImagePreviewDialog
import cn.mine.minestars.ui.context.LocalToaster
import cn.mine.minestars.utils.ImageUtils
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File
import kotlin.uuid.Uuid

@Composable
fun ImageGenPage(
    modifier: Modifier = Modifier,
    vm: ImgGenVM = koinViewModel()
) {
    val pagerState = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()

    val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
    var showCancelDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = pagerState.currentPage != 0 || isGenerating) {
        if (pagerState.currentPage != 0) {
            scope.launch { pagerState.animateScrollToPage(0) }
        } else if (isGenerating) {
            showCancelDialog = true
        }
    }
    if (showCancelDialog) {
        CancelDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = {
                showCancelDialog = false
                vm.cancelGeneration()
            }
        )
    }

    val providerDAO = koinInject<ProviderDAO>()
    val providerEntities by providerDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    val providerSettings = remember(providerEntities) { providerEntities.toProviderSettings() }
    val modelSelectionDao = koinInject<ModelSelectionDAO>()
    val imageGenModelId by modelSelectionDao.getFlow()
        .map { it?.imageGenerationModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() } }
        .collectAsStateWithLifecycle(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (pagerState.currentPage) {
                            0 -> stringResource(R.string.imggen_page_title)
                            1 -> "自定义参数"
                            else -> stringResource(R.string.imggen_page_gallery)
                        }
                    )
                },
                navigationIcon = {
                    if (pagerState.currentPage == 0) {
                        BackButton()
                    } else {
                        IconButton(onClick = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }) {
                            Icon(HugeIcons.ArrowLeft02, "返回")
                        }
                    }
                },
                actions = {}
            )
        },
        bottomBar = {
            BottomBar(pagerState, scope)
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) { page ->
            when (page) {
                0 -> ImageGenScreen(
                    vm = vm,
                    providerSettings = providerSettings,
                    imageGenModelId = imageGenModelId,
                    modelSelectionDao = modelSelectionDao,
                )
                1 -> ParamsPage(
                    vm = vm,
                    providerSettings = providerSettings,
                    imageGenModelId = imageGenModelId,
                )
                2 -> ImageGalleryScreen(vm = vm)
            }
        }
    }
}

@Composable
private fun CancelDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.imggen_page_cancel_generation_title)) },
        text = { Text(stringResource(R.string.imggen_page_cancel_generation_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.imggen_page_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.imggen_page_cancel))
            }
        }
    )
}

@Composable
private fun BottomBar(
    pagerState: PagerState,
    scope: CoroutineScope
) {
    NavigationBar {
        NavigationBarItem(
            selected = 0 == pagerState.currentPage,
            label = { Text(stringResource(R.string.imggen_page_title)) },
            icon = { Icon(HugeIcons.Colors, null) },
            onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
        )

        NavigationBarItem(
            selected = 1 == pagerState.currentPage,
            label = { Text("参数") },
            icon = { Icon(HugeIcons.Tools, null) },
            onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
        )

        NavigationBarItem(
            selected = 2 == pagerState.currentPage,
            label = { Text(stringResource(R.string.imggen_page_gallery)) },
            icon = { Icon(HugeIcons.Image03, null) },
            onClick = { scope.launch { pagerState.animateScrollToPage(2) } }
        )
    }
}

@Composable
private fun ImageGenScreen(
    vm: ImgGenVM,
    providerSettings: List<ProviderSetting>,
    imageGenModelId: Uuid?,
    modelSelectionDao: ModelSelectionDAO,
) {
    val prompt by vm.prompt.collectAsStateWithLifecycle()
    val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
    val currentGeneratedImages by vm.currentGeneratedImages.collectAsStateWithLifecycle()
    val referenceImages by vm.referenceImages.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val settings by vm.settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current

    LaunchedEffect(error) {
        error?.let { errorMessage ->
            toaster.show(message = errorMessage, type = ToastType.Error)
            vm.clearError()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (currentGeneratedImages.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (0 until minOf(2, currentGeneratedImages.size)).forEach { index ->
                        val image = currentGeneratedImages[index]
                        var showPreview by remember { mutableStateOf(false) }
                        AsyncImage(
                            model = File(image.filePath),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showPreview = true },
                            contentScale = ContentScale.Crop
                        )

                        if (showPreview) {
                            ImagePreviewDialog(
                                images = listOf(image.filePath),
                                onDismissRequest = { showPreview = false },
                            )
                        }
                    }
                }
            }
            if (isGenerating) {
                ContainedLoadingIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        InputBar(
            prompt = prompt,
            vm = vm,
            isGenerating = isGenerating,
            referenceImages = referenceImages,
            settings = settings,
            providerSettings = providerSettings,
            imageGenModelId = imageGenModelId,
            modelSelectionDao = modelSelectionDao,
            modifier = Modifier
        )
    }
}

@Composable
private fun InputBar(
    prompt: String,
    vm: ImgGenVM,
    isGenerating: Boolean,
    referenceImages: List<String>,
    settings: Settings,
    providerSettings: List<ProviderSetting>,
    imageGenModelId: Uuid?,
    modelSelectionDao: ModelSelectionDAO,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selectedUris ->
            if (selectedUris.isNotEmpty()) {
                scope.launch {
                    val paths = selectedUris.mapNotNull { uri ->
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val bitmap = ImageUtils.loadOptimizedBitmap(context, uri, maxSize = 2048)
                                    ?: error("Failed to decode image")
                                val pngBytes = FileUtils.compressBitmapToPng(bitmap)
                                bitmap.recycle()
                                val file = File(context.appTempFolder, "imggen_ref_${Uuid.random()}.png")
                                file.writeBytes(pngBytes)
                                file.absolutePath
                            }.getOrNull()
                        }
                    }
                    vm.addReferenceImages(paths)
                }
            }
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (referenceImages.isNotEmpty()) {
            ReferenceImagesRow(
                images = referenceImages,
                onRemove = vm::removeReferenceImage
            )
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = vm::updatePrompt,
            placeholder = { Text(stringResource(R.string.imggen_page_prompt_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 140.dp),
            minLines = 1,
            maxLines = 5,
            shape = MaterialTheme.shapes.large,
            textStyle = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModelSelector(
                modelId = imageGenModelId,
                providers = providerSettings,
                type = ModelType.IMAGE,
                onlyIcon = true,
                onSelect = { model ->
                    scope.launch {
                        val current = modelSelectionDao.get() ?: ModelSelectionEntity()
                        modelSelectionDao.set(current.copy(imageGenerationModelId = model.id.toString()))
                    }
                }
            )

            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") }
            ) {
                Icon(
                    imageVector = HugeIcons.Add01,
                    contentDescription = "Add reference image"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val canSend = prompt.isNotBlank()
            Surface(
                onClick = {
                    if (!isGenerating) {
                        if (referenceImages.isEmpty()) {
                            vm.generateImage()
                        } else {
                            vm.editImage()
                        }
                    } else {
                        vm.cancelGeneration()
                    }
                },
                enabled = isGenerating || canSend,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = when {
                    isGenerating -> MaterialTheme.colorScheme.errorContainer
                    !canSend -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.primary
                },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isGenerating) HugeIcons.Cancel01 else HugeIcons.ArrowUp02,
                        contentDescription = stringResource(R.string.imggen_page_generate_image),
                        tint = when {
                            isGenerating -> MaterialTheme.colorScheme.onErrorContainer
                            !canSend -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else -> MaterialTheme.colorScheme.onPrimary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceImagesRow(
    images: List<String>,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        images.forEach { image ->
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box {
                    AsyncImage(
                        model = File(image),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        onClick = { onRemove(image) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(20.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = HugeIcons.Delete01,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageGalleryScreen(
    vm: ImgGenVM,
) {
    val generatedImages = vm.generatedImages.collectAsLazyPagingItems()
    val context = LocalContext.current
    val filesManager: FilesManager = koinInject()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = { generatedImages.refresh() },
        state = pullToRefreshState
    ) {
        if (generatedImages.itemCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = HugeIcons.Image03,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.imggen_page_no_generated_images),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = generatedImages.itemCount,
                    key = generatedImages.itemKey { it.id },
                    contentType = generatedImages.itemContentType { "GeneratedImage" }
                ) { index ->
                    val image = generatedImages[index]
                    image?.let {
                        var showPreview by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                AsyncImage(
                                    model = File(it.filePath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clickable { showPreview = true },
                                    contentScale = ContentScale.Crop
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = it.model,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = it.prompt.take(20) + if (it.prompt.length > 20) "..." else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("prompt", it.prompt))) }
                                                toaster.show(
                                                    message = "Prompt copied to clipboard",
                                                    type = ToastType.Success
                                                )
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = HugeIcons.Copy01,
                                                contentDescription = "Copy prompt",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        filesManager.saveMessageImage(context, "file://${it.filePath}")
                                                        toaster.show(
                                                            message = context.getString(R.string.imggen_page_image_saved_success),
                                                            type = ToastType.Success
                                                        )
                                                    } catch (e: Exception) {
                                                        toaster.show(
                                                            message = context.getString(
                                                                R.string.imggen_page_save_failed,
                                                                e.message
                                                            ),
                                                            type = ToastType.Error
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = HugeIcons.FloppyDisk,
                                                contentDescription = stringResource(R.string.imggen_page_save),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { vm.deleteImage(it) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = HugeIcons.Delete01,
                                                contentDescription = stringResource(R.string.imggen_page_delete),
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showPreview) {
                            ImagePreviewDialog(
                                images = listOf(it.filePath),
                                onDismissRequest = { showPreview = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamsPage(
    vm: ImgGenVM,
    providerSettings: List<ProviderSetting>,
    imageGenModelId: Uuid?,
) {
    val scope = rememberCoroutineScope()
    val currentModel = remember(providerSettings, imageGenModelId) {
        imageGenModelId?.let { id ->
            providerSettings.firstNotNullOfOrNull { s -> s.models.find { it.id == id } }
        }
    }

    if (currentModel == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("未选择模型", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val model = currentModel
    val baseKeys = remember(model.id, model.paramDefs) {
        model.paramDefs.map { it.name }.toSet()
    }
    val initialEntries = remember(model.id, model.customParams, model.paramDefs) {
        val map = mutableMapOf<String, String>()
        model.paramDefs.forEach { def ->
            map[def.name] = model.customParams[def.name]
                ?: if (def.defaultValue.isNotBlank()) def.defaultValue else ""
        }
        model.customParams.forEach { (key, value) ->
            if (key !in map) map[key] = value
        }
        map.toMap()
    }
    var entries by remember(model.id) { mutableStateOf(initialEntries) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            CardGroup(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                model.paramDefs.forEach { def ->
                    val value = entries[def.name] ?: ""
                    item(
                        supportingContent = {
                            OutlinedTextField(
                                value = value,
                                onValueChange = { newVal ->
                                    entries = entries.toMutableMap().also { it[def.name] = newVal }
                                    scope.launch { vm.updateModelCustomParams(model.id, entries) }
                                },
                                label = { Text(def.name.replace("_", " ")) },
                                placeholder = {
                                    if (def.description.isNotBlank()) {
                                        Text(def.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall,
                            )
                        },
                        headlineContent = {},
                    )
                }

                entries.forEach { (key, value) ->
                    if (key in baseKeys) return@forEach
                    item(
                        supportingContent = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = key,
                                    onValueChange = { newKey ->
                                        val updated = entries.toMutableMap()
                                        updated.remove(key)
                                        if (newKey.isNotBlank()) updated[newKey] = value
                                        entries = updated
                                        scope.launch { vm.updateModelCustomParams(model.id, entries) }
                                    },
                                    label = { Text("参数名") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                )
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { newVal ->
                                        entries = entries.toMutableMap().also { it[key] = newVal }
                                        scope.launch { vm.updateModelCustomParams(model.id, entries) }
                                    },
                                    label = { Text("参数值") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                entries = entries.toMutableMap().also { it.remove(key) }
                                scope.launch { vm.updateModelCustomParams(model.id, entries) }
                            }) {
                                Icon(HugeIcons.Delete01, "删除")
                            }
                        },
                        headlineContent = {},
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    val newKey = "param_${entries.size}"
                    entries = entries.toMutableMap().also { it[newKey] = "" }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(HugeIcons.Add01, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加参数")
            }
        }
    }
}
