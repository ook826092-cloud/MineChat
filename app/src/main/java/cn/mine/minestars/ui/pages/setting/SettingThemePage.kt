package cn.mine.minestars.ui.pages.setting

import android.content.ClipData
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.sonner.ToastType
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.hugeicons.stroke.Delete02
import me.rerere.hugeicons.stroke.Edit02
import me.rerere.hugeicons.stroke.FileImport
import me.rerere.hugeicons.stroke.PlusSign
import me.rerere.hugeicons.stroke.Tick01
import cn.mine.minestars.R
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.components.ui.RikkaConfirmDialog
import cn.mine.minestars.ui.context.LocalToaster
import cn.mine.minestars.ui.pages.setting.components.PresetThemeButtonGroup
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.ui.theme.CustomTheme
import cn.mine.minestars.ui.theme.LocalDarkMode
import cn.mine.minestars.utils.plus
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.uuid.Uuid

private val themeJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingThemePage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val clipboardManager = LocalClipboard.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val exportSuccessMsg = stringResource(R.string.setting_theme_page_export_success)
    val importSuccessMsg = stringResource(R.string.setting_theme_page_import_success)

    var showEditSheet by remember { mutableStateOf(false) }
    var editingTheme by remember { mutableStateOf<CustomTheme?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var deletingTheme by remember { mutableStateOf<CustomTheme?>(null) }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.setting_page_theme_setting)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (settings.dynamicColor) {
                item("dynamicColorHint") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.setting_theme_page_dynamic_color_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!settings.dynamicColor) {
                item("presetThemes") {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.setting_theme_page_preset_themes),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceBright)
                        ) {
                            PresetThemeButtonGroup(
                                themeId = settings.themeId,
                                modifier = Modifier.fillMaxWidth(),
                                onChangeTheme = {
                                    vm.updateSettings(settings.copy(themeId = it))
                                }
                            )
                        }
                    }
                }

                item("customThemesHeader") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.setting_theme_page_custom_themes),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { showImportDialog = true }
                            ) {
                                Icon(HugeIcons.FileImport, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.setting_theme_page_import_theme))
                            }
                            FilledTonalButton(
                                onClick = {
                                    editingTheme = null
                                    showEditSheet = true
                                }
                            ) {
                                Icon(HugeIcons.PlusSign, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.setting_theme_page_add_theme))
                            }
                        }
                    }
                }

                if (settings.customThemes.isEmpty()) {
                    item("emptyCustomThemes") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.setting_theme_page_no_custom_themes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(settings.customThemes, key = { it.id }) { theme ->
                    CustomThemeItem(
                        theme = theme,
                        isSelected = settings.themeId == theme.id,
                        onSelect = {
                            vm.updateSettings(settings.copy(themeId = theme.id))
                        },
                        onExport = {
                            val json = themeJson.encodeToString(theme)
                            scope.launch {
                                clipboardManager.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("theme", json))
                                )
                            }
                            toaster.show(exportSuccessMsg, type = ToastType.Success)
                        },
                        onEdit = {
                            editingTheme = theme
                            showEditSheet = true
                        },
                        onDelete = {
                            deletingTheme = theme
                        }
                    )
                }
            }
        }
    }

    if (showEditSheet) {
        CustomThemeEditSheet(
            theme = editingTheme,
            onDismiss = { showEditSheet = false },
            onSave = { theme ->
                val newThemes = if (editingTheme != null) {
                    settings.customThemes.map { if (it.id == theme.id) theme else it }
                } else {
                    settings.customThemes + theme
                }
                vm.updateSettings(
                    settings.copy(
                        customThemes = newThemes,
                        themeId = theme.id
                    )
                )
                showEditSheet = false
            }
        )
    }

    if (showImportDialog) {
        ImportThemeDialog(
            onDismiss = { showImportDialog = false },
            onImport = { theme ->
                val importedTheme = theme.copy(id = Uuid.random().toString())
                vm.updateSettings(
                    settings.copy(
                        customThemes = settings.customThemes + importedTheme,
                        themeId = importedTheme.id
                    )
                )
                showImportDialog = false
                toaster.show(importSuccessMsg, type = ToastType.Success)
            }
        )
    }

    RikkaConfirmDialog(
        show = deletingTheme != null,
        title = stringResource(R.string.setting_theme_page_delete_theme_title),
        confirmText = stringResource(android.R.string.ok),
        dismissText = stringResource(android.R.string.cancel),
        onConfirm = {
            deletingTheme?.let { theme ->
                val newThemes = settings.customThemes.filter { it.id != theme.id }
                val newThemeId = if (settings.themeId == theme.id) "sakura" else settings.themeId
                vm.updateSettings(settings.copy(customThemes = newThemes, themeId = newThemeId))
            }
            deletingTheme = null
        },
        onDismiss = { deletingTheme = null },
        text = {
            Text(stringResource(R.string.setting_theme_page_delete_theme_message))
        }
    )
}

@Composable
private fun CustomThemeItem(
    theme: CustomTheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val darkMode = LocalDarkMode.current
    val scheme = theme.generateColorScheme(darkMode)

    ListItem(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() },
        headlineContent = { Text(theme.name.ifEmpty { "Unnamed" }) },
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(40.dp)
                ) {
                    drawRect(color = scheme.primaryContainer, size = size)
                    drawRect(
                        color = scheme.secondaryContainer,
                        size = size,
                        topLeft = Offset(x = size.width / 2, y = 0f)
                    )
                    drawRect(
                        color = scheme.tertiaryContainer,
                        size = size,
                        topLeft = Offset(x = size.width / 2, y = size.height / 2)
                    )
                    drawCircle(
                        color = scheme.primary,
                        radius = if (isSelected) 10.dp.toPx() else 6.dp.toPx(),
                        center = Offset(x = size.width / 2, y = size.height / 2)
                    )
                }
                if (isSelected) {
                    Icon(
                        HugeIcons.Tick01,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onExport) {
                    Icon(HugeIcons.Copy01, null)
                }
                IconButton(onClick = onEdit) {
                    Icon(HugeIcons.Edit02, null)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        HugeIcons.Delete02,
                        null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        colors = CustomColors.listItemColors,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomThemeEditSheet(
    theme: CustomTheme?,
    onDismiss: () -> Unit,
    onSave: (CustomTheme) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentTheme by remember {
        mutableStateOf(theme ?: CustomTheme())
    }
    var selectedColorIndex by remember { mutableStateOf(0) } // 0=primary, 1=secondary, 2=tertiary

    val resolveColor: (Int) -> Color = { index ->
        when (index) {
            0 -> Color(currentTheme.primaryColorArgb.toInt())
            1 -> if (currentTheme.secondaryColorArgb != null) Color(currentTheme.secondaryColorArgb!!.toInt())
                 else Color(currentTheme.generateColorScheme(false).secondary.toArgb())
            2 -> if (currentTheme.tertiaryColorArgb != null) Color(currentTheme.tertiaryColorArgb!!.toInt())
                 else Color(currentTheme.generateColorScheme(false).tertiary.toArgb())
            else -> Color.Unspecified
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = if (theme == null) stringResource(R.string.setting_theme_page_create_theme)
                else stringResource(R.string.setting_theme_page_edit_theme),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = currentTheme.name,
                    onValueChange = { currentTheme = currentTheme.copy(name = it) },
                    label = { Text(stringResource(R.string.setting_theme_page_theme_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Preview moved to top
                ThemePreview(currentTheme)

                // Three clickable color circles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ColorCircleButton(
                        color = Color(currentTheme.primaryColorArgb.toInt()),
                        label = stringResource(R.string.setting_theme_page_primary_color),
                        isSelected = selectedColorIndex == 0,
                        onClick = { selectedColorIndex = 0 }
                    )
                    ColorCircleButton(
                        color = if (currentTheme.secondaryColorArgb != null)
                                Color(currentTheme.secondaryColorArgb!!.toInt())
                                else Color(currentTheme.generateColorScheme(false).secondary.toArgb()),
                        label = stringResource(R.string.setting_theme_page_secondary_color),
                        isSelected = selectedColorIndex == 1,
                        onClick = { selectedColorIndex = 1 }
                    )
                    ColorCircleButton(
                        color = if (currentTheme.tertiaryColorArgb != null)
                                Color(currentTheme.tertiaryColorArgb!!.toInt())
                                else Color(currentTheme.generateColorScheme(false).tertiary.toArgb()),
                        label = stringResource(R.string.setting_theme_page_tertiary_color),
                        isSelected = selectedColorIndex == 2,
                        onClick = { selectedColorIndex = 2 }
                    )
                }

                // Shared color picker panel
                ColorPickerPanel(
                    key = selectedColorIndex,
                    color = resolveColor(selectedColorIndex),
                    onColorChange = { color ->
                        val argb = color.toArgb().toLong() and 0xFFFFFFFFL
                        currentTheme = when (selectedColorIndex) {
                            0 -> currentTheme.copy(primaryColorArgb = argb)
                            1 -> currentTheme.copy(secondaryColorArgb = argb)
                            2 -> currentTheme.copy(tertiaryColorArgb = argb)
                            else -> currentTheme
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(currentTheme) },
                    enabled = currentTheme.name.isNotBlank()
                ) {
                    Text(stringResource(R.string.setting_theme_page_save))
                }
            }
        }
    }
}

@Composable
private fun ImportThemeDialog(
    onDismiss: () -> Unit,
    onImport: (CustomTheme) -> Unit,
) {
    var jsonText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_theme_page_import_theme)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {
                        jsonText = it
                        errorMessage = null
                    },
                    label = { Text("JSON") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { msg -> { Text(msg) } },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val theme = themeJson.decodeFromString<CustomTheme>(jsonText)
                        onImport(theme)
                    } catch (e: Exception) {
                        errorMessage = e.message
                    }
                },
                enabled = jsonText.isNotBlank()
            ) {
                Text(stringResource(R.string.setting_theme_page_import_theme))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun ColorCircleButton(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(44.dp)) {
                if (isSelected) {
                    drawCircle(
                        color = primaryColor,
                        radius = 22.dp.toPx(),
                        style = Stroke(3.dp.toPx())
                    )
                }
                drawCircle(
                    color = color,
                    radius = if (isSelected) 18.dp.toPx() else 20.dp.toPx()
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) primaryColor else onSurfaceVariant,
        )
    }
}

@Composable
private fun ColorPickerPanel(
    key: Int,
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    var hue by remember(key) {
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(color.toArgb(), it) }
        mutableFloatStateOf(hsl[0])
    }
    var saturation by remember(key) {
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(color.toArgb(), it) }
        mutableFloatStateOf(hsl[1])
    }
    var lightness by remember(key) {
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(color.toArgb(), it) }
        mutableFloatStateOf(hsl[2])
    }
    var hslCode by remember(key) {
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(color.toArgb(), it) }
        mutableStateOf(formatHslCode(hsl[0], hsl[1], hsl[2]))
    }
    var hslCodeError by remember(key) { mutableStateOf(false) }

    // Sync HSL state when the resolved color changed externally (e.g. derived secondary
    // changed because primary was modified) — without interfering during active dragging.
    val expectedHsl = remember(color.toArgb()) {
        FloatArray(3).also { ColorUtils.colorToHSL(color.toArgb(), it) }
    }
    if (abs(hue - expectedHsl[0]) > 1f || abs(saturation - expectedHsl[1]) > 0.01f || abs(lightness - expectedHsl[2]) > 0.01f) {
        hue = expectedHsl[0]
        saturation = expectedHsl[1]
        lightness = expectedHsl[2]
        hslCode = formatHslCode(expectedHsl[0], expectedHsl[1], expectedHsl[2])
        hslCodeError = false
    }

    fun updateColor(newHue: Float, newSaturation: Float, newLightness: Float) {
        hue = newHue
        saturation = newSaturation
        lightness = newLightness
        hslCode = formatHslCode(newHue, newSaturation, newLightness)
        hslCodeError = false
        onColorChange(Color(ColorUtils.HSLToColor(floatArrayOf(newHue, newSaturation, newLightness))))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 2D Saturation × Lightness picker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val sat = (offset.x / size.width).coerceIn(0f, 1f)
                                val light = (1f - offset.y / size.height).coerceIn(0f, 1f)
                                updateColor(hue, sat, light)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                val light = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                updateColor(hue, sat, light)
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val boxSteps = 100
                for (i in 0 until boxSteps) {
                    val sat = i.toFloat() / (boxSteps - 1)
                    val stripX = i * (w / (boxSteps - 1))
                    val stripW = (w / boxSteps + 1)
                    val topColor = Color(ColorUtils.HSLToColor(floatArrayOf(hue, sat, 1f)))
                    val midColor = Color(ColorUtils.HSLToColor(floatArrayOf(hue, sat, 0.5f)))
                    val bottomColor = Color(ColorUtils.HSLToColor(floatArrayOf(hue, sat, 0f)))
                    drawRect(
                        brush = Brush.verticalGradient(listOf(topColor, midColor, bottomColor)),
                        topLeft = Offset(stripX, 0f),
                        size = Size(stripW, h)
                    )
                }

                // Indicator dot
                val ix = saturation * w
                val iy = (1f - lightness) * h
                drawCircle(Color.White, 10.dp.toPx(), Offset(ix, iy), style = Stroke(3.dp.toPx()))
                drawCircle(Color.Black, 10.dp.toPx(), Offset(ix, iy), style = Stroke(1.5.dp.toPx()))
            }
        }

        // Hue slider bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newHue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                            updateColor(newHue, saturation, lightness)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val newHue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                            updateColor(newHue, saturation, lightness)
                        }
                    )
                }
        ) {
            val barSteps = 360
            for (i in 0 until barSteps) {
                val hc = ColorUtils.HSLToColor(floatArrayOf(i.toFloat(), 1f, 0.5f))
                val barX = i.toFloat() * size.width / barSteps
                val barW = size.width / barSteps
                drawRect(Color(hc), topLeft = Offset(barX, 0f), size = Size(barW, size.height))
            }
            // Indicator
            val ix = (hue / 360f) * size.width
            drawCircle(Color.White, 8.dp.toPx(), Offset(ix, size.height / 2))
            drawCircle(Color.Black, 8.dp.toPx(), Offset(ix, size.height / 2), style = Stroke(2.dp.toPx()))
        }

        // HSL code input
        OutlinedTextField(
            value = hslCode,
            onValueChange = { value ->
                hslCode = value
                val parsedHsl = parseHslCode(value)
                hslCodeError = parsedHsl == null
                if (parsedHsl != null) {
                    hue = parsedHsl[0]
                    saturation = parsedHsl[1]
                    lightness = parsedHsl[2]
                    onColorChange(Color(ColorUtils.HSLToColor(parsedHsl)))
                }
            },
            label = { Text("HSL") },
            placeholder = { Text("hsl(267 36% 48%)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = hslCodeError,
            supportingText = if (hslCodeError) {
                { Text("Use hsl(267 36% 48%)") }
            } else {
                null
            },
        )
    }
}

private val hslNumberRegex = Regex("""[-+]?\d*\.?\d+""")

private fun parseHslCode(value: String): FloatArray? {
    val values = buildList {
        for (match in hslNumberRegex.findAll(value)) {
            add(match.value.toFloatOrNull() ?: return null)
            if (size == 3) break
        }
    }

    if (values.size != 3) return null

    val hue = values[0].coerceIn(0f, 360f)
    val saturation = parseHslPercentOrFraction(values[1]) ?: return null
    val lightness = parseHslPercentOrFraction(values[2]) ?: return null

    return floatArrayOf(hue, saturation, lightness)
}

private fun parseHslPercentOrFraction(value: Float): Float? {
    if (!value.isFinite()) return null
    return if (value > 1f) {
        (value / 100f).coerceIn(0f, 1f)
    } else {
        value.coerceIn(0f, 1f)
    }
}

private fun formatHslCode(hue: Float, saturation: Float, lightness: Float): String {
    return "hsl(${hue.roundToInt()} ${(saturation * 100).roundToInt()}% ${(lightness * 100).roundToInt()}%)"
}

@Composable
private fun ThemePreview(theme: CustomTheme) {
    val darkMode = LocalDarkMode.current
    val scheme = theme.generateColorScheme(darkMode)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.setting_theme_page_preview),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ColorSwatch(scheme.primary, "P")
            ColorSwatch(scheme.secondary, "S")
            ColorSwatch(scheme.tertiary, "T")
            ColorSwatch(scheme.primaryContainer, "PC")
            ColorSwatch(scheme.secondaryContainer, "SC")
            ColorSwatch(scheme.surface, "Sf")
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        ) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
