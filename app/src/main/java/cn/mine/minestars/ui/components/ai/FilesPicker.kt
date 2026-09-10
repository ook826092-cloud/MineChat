package cn.mine.minestars.ui.components.ai

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import cn.mine.ai.provider.ProviderSetting
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Camera01
import me.rerere.hugeicons.stroke.Database01
import me.rerere.hugeicons.stroke.Files02
import me.rerere.hugeicons.stroke.Image02
import me.rerere.hugeicons.stroke.MusicNote03
import me.rerere.hugeicons.stroke.Package
import me.rerere.hugeicons.stroke.Package01
import me.rerere.hugeicons.stroke.Video01
import cn.mine.minestars.R
import cn.mine.minestars.Screen
import cn.mine.minestars.data.ai.mcp.McpManager
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.db.findProvider
import cn.mine.minestars.data.db.toProviderSettings
import kotlin.uuid.Uuid
import cn.mine.minestars.data.ai.mcp.toConfig
import cn.mine.minestars.data.db.dao.KnowledgeBaseDAO
import cn.mine.minestars.data.db.dao.ModelSelectionDAO
import cn.mine.minestars.data.db.dao.McpServerDAO
import cn.mine.minestars.data.db.dao.ProviderDAO
import cn.mine.minestars.core.workspace.WorkspaceShellStatus
import cn.mine.minestars.data.db.dao.WorkspaceDAO
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.data.db.entity.WorkspaceEntity
import cn.mine.minestars.rag.KnowledgeBase
import cn.mine.minestars.data.model.Conversation
import cn.mine.minestars.ui.components.ui.ExtensionSelector
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.context.LocalSettings
import cn.mine.minestars.ui.theme.extendColors
import org.koin.compose.koinInject
import cn.mine.minestars.ui.hooks.ChatInputState
import cn.mine.minestars.data.datastore.SettingsStore
import me.rerere.hugeicons.stroke.Folder01

@Composable
internal fun FilesPicker(
    conversation: Conversation,
    assistant: Assistant,
    state: ChatInputState,
    mcpManager: McpManager,
    enableRag: Boolean = false,
    onToggleRag: () -> Unit = {},
    onCompressContext: (additionalPrompt: String, targetTokens: Int, keepRecentMessages: Int) -> Job,
    onUpdateAssistant: (Assistant) -> Unit,
    onUpdateConversation: (Conversation) -> Unit,
    showInjectionSheet: Boolean,
    onShowInjectionSheetChange: (Boolean) -> Unit,
    showCompressDialog: Boolean,
    onShowCompressDialogChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onTakePic: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onPickAudio: () -> Unit,
    onPickFile: () -> Unit,
) {
    val settings = LocalSettings.current
    val providerDao: ProviderDAO = koinInject()
    val mcpServerDao: McpServerDAO = koinInject()
    val knowledgeBaseDao: KnowledgeBaseDAO = koinInject()
    val workspaceDAO: WorkspaceDAO = koinInject()
    val providerList by providerDao.getAllFlow()
        .map { it.toProviderSettings() }
        .collectAsState(emptyList())
    val modelSelectionDao: ModelSelectionDAO = koinInject()
    val globalModelId by modelSelectionDao.getFlow()
        .map { it?.chatModelId?.let { runCatching { Uuid.parse(it) }.getOrNull() } }
        .collectAsStateWithLifecycle(null)
    val provider = remember(providerList, assistant, globalModelId) {
        val modelId = assistant.chatModelId ?: globalModelId
        providerList.flatMap { it.models }
            .find { it.id == modelId }
            ?.findProvider(providerList)
    }
    val mcpServers by mcpServerDao.getAllFlow()
        .map { entities -> entities.map { it.toConfig() } }
        .collectAsState(emptyList())
    val knowledgeBases by knowledgeBaseDao.getAllFlow()
        .map { entities ->
            entities.map { entity ->
                KnowledgeBase(
                    id = kotlin.uuid.Uuid.parse(entity.id),
                    name = entity.name,
                    embeddingModelId = entity.embeddingModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() },
                    rerankModelId = entity.rerankModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() },
                    topK = entity.topK,
                    similarityThreshold = entity.similarityThreshold,
                    embeddingDimensions = entity.embeddingDimensions,
                )
            }
        }
        .collectAsState(emptyList())

    val workspaceEntities by workspaceDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    val workspaces = remember(workspaceEntities) { workspaceEntities }
    val boundWorkspace = remember(assistant.workspaceId, workspaces) {
        workspaces.find { it.id == assistant.workspaceId }
    }
    var showWorkspaceSheet by remember { mutableStateOf(false) }
    val navController = LocalNavController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TakePicButton(onLaunchCamera = onTakePic)

            ImagePickButton(onClick = onPickImage)

            if (provider != null && provider is ProviderSetting.Google) {
                VideoPickButton(onClick = onPickVideo)

                AudioPickButton(onClick = onPickAudio)
            }

            FilePickButton(onClick = onPickFile)
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth()
        )

        if (mcpServers.isNotEmpty()) {
            McpPickerListItem(
                assistant = assistant,
                servers = mcpServers,
                mcpManager = mcpManager,
                onUpdateAssistant = onUpdateAssistant,
            )
        }

        // RAG / 知识库检索
        val boundKb = assistant.knowledgeBaseId?.let { kbId ->
            knowledgeBases.find { it.id == kbId }
        }
        var showKbSelector by remember { mutableStateOf(false) }
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = HugeIcons.Database01,
                    contentDescription = stringResource(R.string.files_picker_knowledge_base_search),
                )
            },
            headlineContent = {
                Text(stringResource(R.string.files_picker_knowledge_base_search))
            },
            supportingContent = {
                Text(
                    if (boundKb != null) stringResource(R.string.files_picker_knowledge_base_associated, boundKb.name.ifBlank { stringResource(R.string.files_picker_unnamed) }) else stringResource(R.string.files_picker_knowledge_base_unbound),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Switch(
                    checked = enableRag,
                    onCheckedChange = { onToggleRag() },
                )
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable { showKbSelector = true },
        )

        if (showKbSelector) {
            KbSelectorSheet(
                knowledgeBases = knowledgeBases,
                assistant = assistant,
                onUpdateAssistant = onUpdateAssistant,
                onDismiss = { showKbSelector = false },
            )
        }

        // Extensions (Quick Messages + Skills)
        val activeCount =
            assistant.quickMessageIds.size +
                assistant.enabledSkills.size
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = HugeIcons.Package,
                    contentDescription = stringResource(R.string.assistant_page_tab_extensions),
                )
            },
            headlineContent = {
                Text(stringResource(R.string.assistant_page_tab_extensions))
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.files_picker_extensions_manage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                if (activeCount > 0) {
                    Text(
                        text = activeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable {
                    onShowInjectionSheetChange(true)
                },
        )

        // Workspace
        val boundShellStatus = boundWorkspace?.let {
            runCatching { WorkspaceShellStatus.valueOf(it.shellStatus) }.getOrDefault(WorkspaceShellStatus.DISABLED)
        }
        val boundStatusColor = when (boundShellStatus) {
            WorkspaceShellStatus.READY -> MaterialTheme.extendColors.green6
            WorkspaceShellStatus.INSTALLING -> MaterialTheme.extendColors.orange5
            WorkspaceShellStatus.BROKEN -> MaterialTheme.extendColors.red6
            else -> MaterialTheme.extendColors.gray5
        }
        ListItem(
            leadingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (boundWorkspace != null) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(boundStatusColor)
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    Icon(
                        imageVector = HugeIcons.Folder01,
                        contentDescription = stringResource(R.string.extensions_page_workspace),
                    )
                }
            },
            headlineContent = {
                Text(stringResource(R.string.extensions_page_workspace))
            },
            supportingContent = {
                Text(
                    if (boundWorkspace != null) boundWorkspace.name else stringResource(R.string.files_picker_workspace_unbound),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                if (assistant.workspaceId != null) {
                    Switch(
                        checked = true,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                onUpdateAssistant(assistant.copy(workspaceId = null))
                            }
                        },
                    )
                }
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable { showWorkspaceSheet = true },
        )

        if (showWorkspaceSheet) {
            WorkspaceSelectSheet(
                workspaces = workspaces,
                assistant = assistant,
                onSelect = { id ->
                    onUpdateAssistant(assistant.copy(workspaceId = id))
                    showWorkspaceSheet = false
                },
                onManage = {
                    showWorkspaceSheet = false
                    navController.navigate(Screen.Workspaces)
                },
                onDismiss = { showWorkspaceSheet = false },
            )
        }

        // Compress History Button
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = HugeIcons.Package01,
                    contentDescription = stringResource(R.string.chat_page_compress_context),
                )
            },
            headlineContent = {
                Text(stringResource(R.string.chat_page_compress_context))
            },
            supportingContent = {
                Text(
                    text = stringResource(R.string.files_picker_compress_context_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                if (conversation.messageNodes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_page_message_count, conversation.messageNodes.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable {
                    onShowCompressDialogChange(true)
                },
        )
    }

    // Injection Bottom Sheet
    if (showInjectionSheet) {
        InjectionQuickConfigSheet(
            conversation = conversation,
            assistant = assistant,
            settings = settings,
            onUpdateAssistant = onUpdateAssistant,
            onUpdateConversation = onUpdateConversation,
            onDismiss = { onShowInjectionSheetChange(false) })
    }

    // Compress Context Dialog
    if (showCompressDialog) {
        CompressContextDialog(onDismiss = {
            onShowCompressDialogChange(false)
            onDismiss()
        }, onConfirm = { additionalPrompt, targetTokens, keepRecentMessages ->
            onCompressContext(additionalPrompt, targetTokens, keepRecentMessages)
        })
    }
}

@Composable
private fun InjectionQuickConfigSheet(
    conversation: Conversation,
    assistant: Assistant,
    settings: Settings,
    onUpdateAssistant: (Assistant) -> Unit,
    onUpdateConversation: (Conversation) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 16.dp),
        ) {
            ExtensionSelector(
                assistant = assistant,
                settings = settings,
                onUpdate = onUpdateAssistant,
                conversation = conversation,
                onUpdateConversation = onUpdateConversation,
                modifier = Modifier.weight(1f),
                onNavigateToQuickMessages = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                        navController.navigate(Screen.QuickMessages)
                    }
                },
                onNavigateToSkills = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                        navController.navigate(Screen.Skills)
                    }
                })

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ImagePickButton(onClick: () -> Unit = {}) {
    BigIconTextButton(icon = {
        Icon(HugeIcons.Image02, null)
    }, text = {
        Text(stringResource(R.string.photo))
    }) {
        onClick()
    }
}

@Composable
fun TakePicButton(onLaunchCamera: () -> Unit = {}) {
    BigIconTextButton(icon = {
        Icon(HugeIcons.Camera01, null)
    }, text = {
        Text(stringResource(R.string.take_picture))
    }) {
        onLaunchCamera()
    }
}

@Composable
fun VideoPickButton(onClick: () -> Unit = {}) {
    BigIconTextButton(icon = {
        Icon(HugeIcons.Video01, null)
    }, text = {
        Text(stringResource(R.string.video))
    }) {
        onClick()
    }
}

@Composable
fun AudioPickButton(onClick: () -> Unit = {}) {
    BigIconTextButton(icon = {
        Icon(HugeIcons.MusicNote03, null)
    }, text = {
        Text(stringResource(R.string.audio))
    }) {
        onClick()
    }
}

@Composable
fun FilePickButton(onClick: () -> Unit = {}) {
    BigIconTextButton(icon = {
        Icon(HugeIcons.Files02, null)
    }, text = {
        Text(stringResource(R.string.upload_file))
    }) {
        onClick()
    }
}

@Composable
private fun KbSelectorSheet(
    knowledgeBases: List<KnowledgeBase>,
    assistant: Assistant,
    onUpdateAssistant: (Assistant) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.files_picker_select_knowledge_base),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            // 不绑定
            ListItem(
                headlineContent = { Text(stringResource(R.string.files_picker_no_bind)) },
                leadingContent = {
                    RadioButton(
                        selected = assistant.knowledgeBaseId == null,
                        onClick = {
                            onUpdateAssistant(assistant.copy(knowledgeBaseId = null))
                            onDismiss()
                        },
                    )
                },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .clickable {
                        onUpdateAssistant(assistant.copy(knowledgeBaseId = null))
                        onDismiss()
                    },
            )

            if (knowledgeBases.isEmpty()) {
                Text(
                    stringResource(R.string.files_picker_no_knowledge_bases),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                knowledgeBases.forEach { kb ->
                    ListItem(
                        headlineContent = { Text(kb.name.ifBlank { stringResource(R.string.files_picker_unnamed_knowledge_base) }) },
                        supportingContent = { Text(stringResource(R.string.files_picker_file_count, kb.files.size)) },
                        leadingContent = {
                            RadioButton(
                                selected = assistant.knowledgeBaseId == kb.id,
                                onClick = {
                                    onUpdateAssistant(assistant.copy(knowledgeBaseId = kb.id))
                                    onDismiss()
                                },
                            )
                        },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .clickable {
                                onUpdateAssistant(assistant.copy(knowledgeBaseId = kb.id))
                                onDismiss()
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun BigIconTextButton(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick
            )
            .semantics {
                role = Role.Button
            }
            .wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Surface(
            tonalElevation = 2.dp, shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                icon()
            }
        }
        ProvideTextStyle(MaterialTheme.typography.bodySmall) {
            text()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BigIconTextButtonPreview() {
    Row(
        modifier = Modifier.padding(16.dp)
    ) {
        BigIconTextButton(icon = {
            Icon(HugeIcons.Image02, null)
        }, text = {
            Text(stringResource(R.string.photo))
        }) {}
    }
}
