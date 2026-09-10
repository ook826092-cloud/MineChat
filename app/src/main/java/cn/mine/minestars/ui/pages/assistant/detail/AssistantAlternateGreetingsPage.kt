package cn.mine.minestars.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import cn.mine.minestars.ui.components.ui.RikkaConfirmDialog
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.plus
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.FullScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AssistantAlternateGreetingsPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(id) })
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var deleteIndex by remember { mutableStateOf<Int?>(null) }

    // 确保至少有一个空条目
    val greetings = remember(assistant.alternateGreetings) {
        if (assistant.alternateGreetings.isEmpty()) listOf("") else assistant.alternateGreetings
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.assistant_greetings_title)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    vm.update(assistant.copy(alternateGreetings = greetings + ""))
                }
            ) {
                Icon(HugeIcons.Add01, contentDescription = stringResource(R.string.assistant_greetings_add))
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(greetings, key = { index, _ -> "alt_$index" }) { index, text ->
                AlternateGreetingCard(
                    index = index,
                    text = text,
                    onTextChange = { newText ->
                        val newList = greetings.toMutableList()
                        newList[index] = newText
                        vm.update(assistant.copy(alternateGreetings = newList))
                    },
                    onDelete = { deleteIndex = index },
                )
            }
        }
    }

    RikkaConfirmDialog(
        show = deleteIndex != null,
        title = stringResource(R.string.assistant_greetings_delete_title),
        confirmText = stringResource(R.string.assistant_greetings_delete),
        dismissText = stringResource(R.string.assistant_greetings_cancel),
        onConfirm = {
            deleteIndex?.let { idx ->
                val newList = greetings.toMutableList()
                newList.removeAt(idx)
                vm.update(assistant.copy(alternateGreetings = newList))
            }
            deleteIndex = null
        },
        onDismiss = { deleteIndex = null },
    ) {
        Text(stringResource(R.string.assistant_greetings_delete_confirm, (deleteIndex ?: 0) + 1))
    }
}

@Composable
private fun AlternateGreetingCard(
    index: Int,
    text: String,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
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
                    text = stringResource(R.string.assistant_greetings_item_label, index + 1),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showFullscreen = true }) {
                    Icon(
                        imageVector = HugeIcons.FullScreen,
                        contentDescription = stringResource(R.string.assistant_greetings_fullscreen_edit),
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (index > 0) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = HugeIcons.Delete01,
                            contentDescription = stringResource(R.string.assistant_greetings_delete_content_desc),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("alternateGreetings[$index]", style = MaterialTheme.typography.bodySmall) },
                shape = RoundedCornerShape(14.dp),
                minLines = 2,
                maxLines = 6,
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
                    var editingText by remember { mutableStateOf(text) }

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
                                    text = stringResource(R.string.assistant_greetings_item_label, index + 1),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = "alternateGreetings[$index]",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = {
                                onTextChange(editingText)
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
