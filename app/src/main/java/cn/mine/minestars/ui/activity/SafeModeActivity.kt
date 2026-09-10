package cn.mine.minestars.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cn.mine.minestars.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.dao.TagDAO
import cn.mine.minestars.data.db.entity.TagEntity
import cn.mine.minestars.data.db.toModel
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.ui.hooks.writeStringPreference
import cn.mine.minestars.ui.theme.MineChatTheme
import org.koin.compose.koinInject
import cn.mine.minestars.utils.CrashHandler
import org.koin.android.ext.android.inject
import kotlin.uuid.Uuid

class SafeModeActivity : ComponentActivity() {
    private val settingsStore by inject<SettingsStore>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stackTrace = CrashHandler.getStackTrace(this)
        CrashHandler.clearCrashed(this)
        enableEdgeToEdge()
        setContent {
            MineChatTheme {
                val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
                var showAssistantPicker by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                val assistantDao = koinInject<AssistantDAO>()
                val tagDao = koinInject<TagDAO>()

                var currentAssistant by remember { mutableStateOf<Assistant?>(null) }
                LaunchedEffect(settings.assistantId) {
                    currentAssistant = assistantDao.getById(settings.assistantId.toString())?.toModel()
                }

                val assistantEntities by assistantDao.getAllFlow()
                    .collectAsStateWithLifecycle(emptyList())
                val assistants by remember(assistantEntities) {
                    derivedStateOf { assistantEntities.map { it.toModel() } }
                }

                val tagEntities by tagDao.getAllFlow()
                    .collectAsStateWithLifecycle(emptyList())

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = { Text(stringResource(R.string.safe_mode_title)) })
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.safe_mode_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = stringResource(
                                R.string.safe_mode_current_assistant,
                                currentAssistant?.name?.ifEmpty { stringResource(R.string.safe_mode_default_assistant) }
                                    ?: stringResource(R.string.safe_mode_default_assistant)),
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Button(
                            onClick = { showAssistantPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.safe_mode_switch_assistant))
                        }

                        if (stackTrace != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.safe_mode_crash_report),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                OutlinedButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("crash", stackTrace))
                                    }
                                ) {
                                    Text(stringResource(R.string.safe_mode_copy))
                                }
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                val vScroll = rememberScrollState()
                                val hScroll = rememberScrollState()
                                Text(
                                    text = stackTrace,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .verticalScroll(vScroll)
                                        .horizontalScroll(hScroll),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }

                if (showAssistantPicker) {
                    AssistantPickerSheet(
                        currentAssistantId = settings.assistantId,
                        assistants = assistants,
                        assistantTags = tagEntities,
                        onAssistantSelected = { assistantId ->
                            scope.launch { settingsStore.updateAssistant(assistantId) }
                            context.writeStringPreference("lastConversationId", null)
                            showAssistantPicker = false
                        },
                        onDismiss = { showAssistantPicker = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantPickerSheet(
    currentAssistantId: Uuid,
    assistants: List<Assistant>,
    assistantTags: List<TagEntity>,
    onAssistantSelected: (Uuid) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTagIds by remember { mutableStateOf(emptySet<Uuid>()) }
    val filteredAssistants = remember(assistants, selectedTagIds) {
        if (selectedTagIds.isEmpty()) assistants
        else assistants.filter { it.tags.any { id -> id in selectedTagIds } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.safe_mode_assistants),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (assistantTags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(assistantTags, key = { it.id }) { tag ->
                        val tagUuid = remember(tag.id) { Uuid.parse(tag.id) }
                        FilterChip(
                            onClick = {
                                selectedTagIds = if (tagUuid in selectedTagIds) {
                                    selectedTagIds - tagUuid
                                } else {
                                    selectedTagIds + tagUuid
                                }
                            },
                            label = { Text(tag.name) },
                            selected = tagUuid in selectedTagIds,
                            shape = RoundedCornerShape(50),
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredAssistants, key = { it.id }) { assistant ->
                    val checked = assistant.id == currentAssistantId
                    Card(
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onAssistantSelected(assistant.id)
                            }
                        },
                        modifier = Modifier.animateItem(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            contentColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text(
                            text = assistant.name.ifEmpty { stringResource(R.string.safe_mode_default_assistant) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
