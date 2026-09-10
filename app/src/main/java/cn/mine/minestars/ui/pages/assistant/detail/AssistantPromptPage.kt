package cn.mine.minestars.ui.pages.assistant.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import cn.mine.minestars.Screen
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.theme.CustomColors
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowDown01
import me.rerere.hugeicons.stroke.ArrowRight01
import me.rerere.hugeicons.stroke.ArrowUp01
import me.rerere.hugeicons.stroke.Book01
import me.rerere.hugeicons.stroke.FullScreen
import me.rerere.hugeicons.stroke.Message02
import me.rerere.hugeicons.stroke.Puzzle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AssistantPromptPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(
        parameters = {
            parametersOf(id)
        }
    )
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.assistant_prompt_character_card)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        AssistantPromptContent(
            modifier = Modifier.padding(innerPadding),
            assistant = assistant,
            onUpdate = { vm.update(it) }
        )
    }
}

@Composable
private fun AssistantPromptContent(
    modifier: Modifier = Modifier,
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    val navController = LocalNavController.current
    var showAdvanced by rememberSaveable(assistant.id) { mutableStateOf(false) }

    // Local state for instant text field response (avoids cursor jump on async ViewModel sync)
    var charNameText by remember(assistant.id) { mutableStateOf(assistant.charName) }
    var descriptionText by remember(assistant.id) { mutableStateOf(assistant.description) }
    var greetingText by remember(assistant.id) { mutableStateOf(assistant.firstMessage) }
    var personalityText by remember(assistant.id) { mutableStateOf(assistant.personality) }
    var scenarioText by remember(assistant.id) { mutableStateOf(assistant.scenario) }
    var mesExamplesText by remember(assistant.id) { mutableStateOf(assistant.mesExamples) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PromptField(
                title = stringResource(R.string.assistant_prompt_char_name),
                label = "charName",
                value = charNameText,
                onValueChange = {
                    charNameText = it
                    onUpdate(assistant.copy(charName = it, name = it))
                },
                singleLine = true
            )

            PromptField(
                title = stringResource(R.string.assistant_prompt_description),
                label = "description",
                value = descriptionText,
                onValueChange = {
                    descriptionText = it
                    onUpdate(assistant.copy(description = it))
                },
                minLines = 3,
                maxLines = 6,
                enableFullscreen = true
            )

            PromptField(
                title = stringResource(R.string.assistant_prompt_greeting),
                label = "firstMessage",
                value = greetingText,
                onValueChange = {
                    greetingText = it
                    onUpdate(assistant.copy(firstMessage = it))
                },
                minLines = 3,
                maxLines = 8,
                enableFullscreen = true
            )

            // 备用开场白入口
            EntryCard(
                title = stringResource(R.string.assistant_prompt_alt_greetings),
                icon = HugeIcons.Puzzle,
                description = stringResource(R.string.assistant_prompt_alt_greetings_count, assistant.alternateGreetings.size),
                onClick = { navController.navigate(Screen.AssistantAlternateGreetings(assistant.id.toString())) }
            )

            // 可折叠的更多角色字段
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(HugeIcons.Puzzle, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(stringResource(R.string.assistant_prompt_more_fields), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.assistant_prompt_more_fields_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        if (showAdvanced) HugeIcons.ArrowUp01 else HugeIcons.ArrowDown01,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(showAdvanced) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PromptField(
                            title = stringResource(R.string.assistant_prompt_personality),
                            label = "personality",
                            value = personalityText,
                            onValueChange = {
                                personalityText = it
                                onUpdate(assistant.copy(personality = it))
                            },
                            minLines = 3,
                            maxLines = 6,
                            enableFullscreen = true
                        )
                        PromptField(
                            title = stringResource(R.string.assistant_prompt_backstory),
                            label = "scenario",
                            value = scenarioText,
                            onValueChange = {
                                scenarioText = it
                                onUpdate(assistant.copy(scenario = it))
                            },
                            minLines = 3,
                            maxLines = 6,
                            enableFullscreen = true
                        )
                        PromptField(
                            title = stringResource(R.string.assistant_prompt_examples),
                            label = "mesExamples",
                            value = mesExamplesText,
                            onValueChange = {
                                mesExamplesText = it
                                onUpdate(assistant.copy(mesExamples = it))
                            },
                            minLines = 5,
                            maxLines = 15,
                            placeholder = stringResource(R.string.assistant_prompt_examples_placeholder),
                            enableFullscreen = true
                        )
                    }
                }
            }

            // 更多设置入口
            Text(
                text = stringResource(R.string.assistant_prompt_more_settings),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )

            EntryCard(
                title = stringResource(R.string.assistant_prompt_extra_fields),
                icon = HugeIcons.Puzzle,
                description = stringResource(R.string.assistant_prompt_extra_fields_desc),
                onClick = { navController.navigate(Screen.AssistantCardExtensions(assistant.id.toString())) }
            )
            EntryCard(
                title = stringResource(R.string.assistant_prompt_system_instructions),
                icon = HugeIcons.Message02,
                description = stringResource(R.string.assistant_prompt_system_instructions_desc),
                onClick = { navController.navigate(Screen.AssistantCardSystem(assistant.id.toString())) }
            )
            EntryCard(
                title = stringResource(R.string.assistant_prompt_additional_info),
                icon = HugeIcons.Book01,
                description = stringResource(R.string.assistant_prompt_additional_info_desc),
                onClick = { navController.navigate(Screen.AssistantCardMeta(assistant.id.toString())) }
            )
        }
    }
}

@Composable
private fun EntryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(HugeIcons.ArrowRight01, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PromptField(
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
                            contentDescription = stringResource(R.string.assistant_prompt_fullscreen_edit),
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
        FullScreenEditDialog(
            title = title,
            label = label,
            initialText = value,
            onDismiss = { showFullscreen = false },
            onConfirm = {
                onValueChange(it)
                showFullscreen = false
            }
        )
    }
}

@Composable
private fun FullScreenEditDialog(
    title: String,
    label: String,
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
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
                        TextButton(onClick = { onConfirm(text) }) {
                            Text(stringResource(R.string.chat_page_save))
                        }
                    }

                    TextField(
                        value = text,
                        onValueChange = { text = it },
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
