package cn.mine.minestars.ui.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cn.mine.minestars.R
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.db.dao.QuickMessageDAO
import cn.mine.minestars.data.files.SkillManager
import cn.mine.minestars.data.files.SkillMetadata
import cn.mine.minestars.data.model.Assistant
import cn.mine.minestars.data.model.Conversation
import cn.mine.minestars.data.model.QuickMessage
import cn.mine.minestars.ui.components.ai.ExtensionEmptyState
import cn.mine.minestars.ui.components.ai.QuickMessagesContent
import cn.mine.minestars.ui.components.ai.SkillsContent
import org.koin.compose.koinInject


@Composable
fun ExtensionSelector(
    modifier: Modifier = Modifier,
    assistant: Assistant,
    settings: Settings,
    onUpdate: (Assistant) -> Unit,
    conversation: Conversation? = null,
    onUpdateConversation: ((Conversation) -> Unit)? = null,
    onNavigateToQuickMessages: () -> Unit = {},
    onNavigateToSkills: () -> Unit = {},
) {
    val skillManager: SkillManager = koinInject()
    val quickMessageDAO: QuickMessageDAO = koinInject()
    val quickMessages by quickMessageDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    var skills by remember { mutableStateOf<List<SkillMetadata>>(emptyList()) }
    val quickMessageModels = remember(quickMessages) {
        quickMessages.map { QuickMessage(id = kotlin.uuid.Uuid.parse(it.id), title = it.title, content = it.content) }
    }

    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 4.dp,
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(0) }
                },
                text = { Text(stringResource(R.string.extension_selector_tab_quick_messages)) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(1) }
                },
                text = { Text(stringResource(R.string.extension_selector_tab_skills)) }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> {
                    if (quickMessageModels.isNotEmpty()) {
                        QuickMessagesContent(
                            quickMessages = quickMessageModels,
                            selectedIds = assistant.quickMessageIds,
                            onToggle = { id, checked ->
                                val newIds = if (checked) {
                                    assistant.quickMessageIds + id
                                } else {
                                    assistant.quickMessageIds - id
                                }
                                onUpdate(assistant.copy(quickMessageIds = newIds))
                            },
                            onManage = onNavigateToQuickMessages,
                        )
                    } else {
                        ExtensionEmptyState(
                            message = stringResource(R.string.extension_selector_quick_messages_empty),
                            buttonText = stringResource(R.string.extension_selector_go_to_extensions),
                            onAction = onNavigateToQuickMessages,
                        )
                    }
                }

                1 -> {
                    if (skills.isNotEmpty()) {
                        SkillsContent(
                            skills = skills,
                            enabledSkills = assistant.enabledSkills,
                            onToggle = { name, checked ->
                                val newSkills = if (checked) {
                                    assistant.enabledSkills + name
                                } else {
                                    assistant.enabledSkills - name
                                }
                                onUpdate(assistant.copy(enabledSkills = newSkills))
                            },
                            onManage = onNavigateToSkills,
                        )
                    } else {
                        ExtensionEmptyState(
                            message = stringResource(R.string.extension_selector_skills_empty),
                            buttonText = stringResource(R.string.extension_selector_go_to_skills),
                            onAction = onNavigateToSkills,
                        )
                    }
                }
            }
        }
    }
}
