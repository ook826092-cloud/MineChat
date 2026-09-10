package cn.mine.minestars.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.mine.minestars.R
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.theme.CustomColors
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.FullScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// ── 扩展字段：昵称、群聊开场白 ──
@Composable
fun AssistantCardExtensionsPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(id) })
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.assistant_card_extra_fields)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubFieldCard(
                    title = stringResource(R.string.assistant_card_nickname),
                    label = "nickname",
                    value = assistant.nickname,
                    placeholder = stringResource(R.string.assistant_card_nickname_placeholder),
                    onValueChange = { vm.update(assistant.copy(nickname = it)) },
                )

                val groupText = assistant.groupOnlyGreetings.joinToString("\n")
                SubFieldCard(
                    title = stringResource(R.string.assistant_card_group_greeting),
                    label = "groupOnlyGreetings",
                    value = groupText,
                    onValueChange = {
                        vm.update(
                            assistant.copy(groupOnlyGreetings = it.lines().filter { line -> line.isNotBlank() })
                        )
                    },
                    minLines = 2,
                    maxLines = 6,
                    enableFullscreen = true,
                )
            }
        }
    }
}

// ── 系统指令：Main Prompt、后置指令 ──
@Composable
fun AssistantCardSystemPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(id) })
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.assistant_card_system_instructions)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubFieldCard(
                    title = "Main Prompt",
                    label = "mainPromptOverride",
                    value = assistant.mainPromptOverride,
                    placeholder = stringResource(R.string.assistant_card_system_instructions_placeholder),
                    onValueChange = { vm.update(assistant.copy(mainPromptOverride = it)) },
                    minLines = 3,
                    maxLines = 8,
                    enableFullscreen = true,
                )

                SubFieldCard(
                    title = "Post-History Instructions",
                    label = "postHistoryInstructions",
                    value = assistant.postHistoryInstructions,
                    onValueChange = { vm.update(assistant.copy(postHistoryInstructions = it)) },
                    minLines = 2,
                    maxLines = 6,
                    enableFullscreen = true,
                )
            }
        }
    }
}

// ── 附加信息：版本、作者、来源、备注、标签 ──
@Composable
fun AssistantCardMetaPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(id) })
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.assistant_card_additional_info)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubFieldCard(
                    title = stringResource(R.string.assistant_card_version),
                    label = "characterVersion",
                    value = assistant.characterVersion,
                    onValueChange = { vm.update(assistant.copy(characterVersion = it)) },
                )

                SubFieldCard(
                    title = stringResource(R.string.assistant_card_author),
                    label = "creator",
                    value = assistant.creator,
                    onValueChange = { vm.update(assistant.copy(creator = it)) },
                )

                SubFieldCard(
                    title = stringResource(R.string.assistant_card_source),
                    label = "source",
                    value = assistant.source,
                    placeholder = stringResource(R.string.assistant_card_source_placeholder),
                    onValueChange = { vm.update(assistant.copy(source = it)) },
                )

                SubFieldCard(
                    title = stringResource(R.string.assistant_card_notes),
                    label = "creatorNotes",
                    value = assistant.creatorNotes,
                    onValueChange = { vm.update(assistant.copy(creatorNotes = it)) },
                    minLines = 2,
                    maxLines = 6,
                    enableFullscreen = true,
                )

                val tagsText = assistant.cardTags.joinToString(", ")
                SubFieldCard(
                    title = stringResource(R.string.assistant_card_tags),
                    label = "cardTags",
                    value = tagsText,
                    placeholder = stringResource(R.string.assistant_card_tags_placeholder),
                    onValueChange = {
                        val normalized = it.split(",")
                            .map { item -> item.trim() }
                            .filter { item -> item.isNotBlank() }
                        vm.update(assistant.copy(cardTags = normalized))
                    },
                    enableFullscreen = true,
                )
            }
        }
    }
}

// ── 共享的字段卡片组件 ──

@Composable
private fun SubFieldCard(
    title: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
    maxLines: Int = 5,
    singleLine: Boolean = false,
    placeholder: String? = null,
    enableFullscreen: Boolean = false,
) {
    var showFullscreen by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (enableFullscreen) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showFullscreen = true }) {
                        Icon(
                            imageVector = HugeIcons.FullScreen,
                            contentDescription = stringResource(R.string.assistant_card_fullscreen_edit),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                shape = RoundedCornerShape(14.dp),
                minLines = if (singleLine) 1 else minLines,
                maxLines = if (singleLine) 1 else maxLines,
                singleLine = singleLine,
            )
        }
    }

    if (showFullscreen) {
        BasicAlertDialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding(),
                verticalArrangement = Arrangement.Bottom
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                ) {
                    var editingText by remember { mutableStateOf(value) }

                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = {
                                onValueChange(editingText)
                                showFullscreen = false
                            }) {
                                Text(stringResource(R.string.chat_page_save))
                            }
                        }

                        TextField(
                            value = editingText,
                            onValueChange = { editingText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(32.dp),
                            colors = TextFieldDefaults.colors().copy(
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        }
    }
}
