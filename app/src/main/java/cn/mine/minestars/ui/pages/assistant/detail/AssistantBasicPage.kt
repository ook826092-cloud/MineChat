package cn.mine.minestars.ui.pages.assistant.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.mine.ai.provider.ModelType
import cn.mine.minestars.R
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.rag.KnowledgeBase
import cn.mine.minestars.ui.components.ai.ModelSelector
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.components.ui.FormItem
import cn.mine.minestars.ui.components.ui.TagsInput
import cn.mine.minestars.ui.components.ui.UIAvatar
import cn.mine.minestars.ui.hooks.heroAnimation
import cn.mine.minestars.ui.theme.CustomColors
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Database01
import cn.mine.minestars.utils.toFixed
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt
import cn.mine.minestars.data.model.Tag as DataTag

@Composable
fun AssistantBasicPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(
        parameters = {
            parametersOf(id)
        }
    )
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val knowledgeBases by vm.knowledgeBases.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(stringResource(R.string.assistant_page_tab_basic))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        AssistantBasicContent(
            modifier = Modifier.padding(innerPadding),
            assistant = assistant,
            providers = providers,
            tags = tags,
            knowledgeBases = knowledgeBases,
            onUpdate = { vm.update(it) },
            vm = vm
        )
    }
}

@Composable
internal fun AssistantBasicContent(
    modifier: Modifier = Modifier,
    assistant: Assistant,
    providers: List<cn.mine.ai.provider.ProviderSetting>,
    tags: List<DataTag>,
    knowledgeBases: List<KnowledgeBase> = emptyList(),
    onUpdate: (Assistant) -> Unit,
    vm: AssistantDetailVM
) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UIAvatar(
                value = assistant.avatar,
                name = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                onUpdate = { avatar ->
                    onUpdate(
                        assistant.copy(
                            avatar = avatar
                        )
                    )
                },
                modifier = Modifier
                    .size(80.dp)
                    .heroAnimation("assistant_${assistant.id}")
            )
        }

        Card(
            colors = CustomColors.cardColorsOnSurfaceContainer
        ) {
            FormItem(
                label = {
                    Text(stringResource(R.string.assistant_page_tags))
                },
                modifier = Modifier.padding(8.dp),
            ) {
                TagsInput(
                    value = assistant.tags,
                    tags = tags,
                    onValueChange = { tagIds, tagList ->
                        vm.updateTags(tagIds, tagList)
                    },
                )
            }

            HorizontalDivider()

            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_use_assistant_avatar))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_use_assistant_avatar_desc))
                },
                tail = {
                    Switch(
                        checked = assistant.useAssistantAvatar,
                        onCheckedChange = {
                            onUpdate(
                                assistant.copy(
                                    useAssistantAvatar = it
                                )
                            )
                        }
                    )
                }
            )
        }

        Card(
            colors = CustomColors.cardColorsOnSurfaceContainer
        ) {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_chat_model))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_chat_model_desc))
                },
                content = {
                    ModelSelector(
                        modelId = assistant.chatModelId,
                        providers = providers,
                        type = ModelType.CHAT,
                        onSelect = {
                            onUpdate(
                                assistant.copy(
                                    chatModelId = it.id
                                )
                            )
                        },
                    )
                }
            )
        }

        Card(
            colors = CustomColors.cardColorsOnSurfaceContainer
        ) {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = { Text(stringResource(R.string.assistant_basic_gradient_bg)) },
                description = { Text(stringResource(R.string.assistant_basic_gradient_bg_desc)) },
                tail = {
                    Switch(
                        checked = assistant.useGradientBackground,
                        onCheckedChange = {
                            onUpdate(assistant.copy(useGradientBackground = it))
                        }
                    )
                }
            )

            if (!assistant.useGradientBackground) {
                HorizontalDivider()

                BackgroundPicker(
                    modifier = Modifier.padding(8.dp),
                    background = assistant.background,
                    backgroundOpacity = assistant.backgroundOpacity,
                    onUpdate = { background ->
                        onUpdate(
                            assistant.copy(
                                background = background
                            )
                        )
                    }
                )

                if (assistant.background != null) {
                    val backgroundOpacity = assistant.backgroundOpacity.coerceIn(0f, 1f)
                    HorizontalDivider()
                    FormItem(
                        modifier = Modifier.padding(8.dp),
                        label = {
                            Text(stringResource(R.string.assistant_page_background_opacity))
                        },
                        description = {
                            Text(stringResource(R.string.assistant_page_background_opacity_desc))
                        }
                    ) {
                        Slider(
                            value = backgroundOpacity,
                            onValueChange = {
                                onUpdate(
                                    assistant.copy(
                                        backgroundOpacity = it.toFixed(2).toFloatOrNull()?.coerceIn(0f, 1f) ?: 1.0f
                                    )
                                )
                            },
                            valueRange = 0f..1f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(
                                R.string.assistant_page_background_opacity_value,
                                (backgroundOpacity * 100).roundToInt()
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }

        // 知识库绑定
        var kbExpanded by remember { mutableStateOf(false) }
        val currentKb = knowledgeBases.find { it.id == assistant.knowledgeBaseId }
        Card(
            colors = CustomColors.cardColorsOnSurfaceContainer
        ) {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = { Text(stringResource(R.string.assistant_basic_knowledge_base)) },
                description = { Text(stringResource(R.string.assistant_basic_knowledge_base_desc)) },
                content = {
                    androidx.compose.material3.Surface(
                        onClick = { kbExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(HugeIcons.Database01, null)
                                Text(
                                    text = currentKb?.name?.ifBlank { stringResource(R.string.assistant_basic_unnamed_kb) } ?: stringResource(R.string.assistant_basic_unbound),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            DropdownMenu(
                                expanded = kbExpanded,
                                onDismissRequest = { kbExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.assistant_basic_no_bind)) },
                                    onClick = {
                                        onUpdate(assistant.copy(knowledgeBaseId = null))
                                        kbExpanded = false
                                    },
                                )
                                knowledgeBases.forEach { kb ->
                                    DropdownMenuItem(
                                        text = { Text(kb.name.ifBlank { stringResource(R.string.assistant_basic_unnamed_kb) }) },
                                        onClick = {
                                            onUpdate(assistant.copy(knowledgeBaseId = kb.id))
                                            kbExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
    }
}
