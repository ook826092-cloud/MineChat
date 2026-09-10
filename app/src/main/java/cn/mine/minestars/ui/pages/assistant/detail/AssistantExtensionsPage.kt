package cn.mine.minestars.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextButton
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.mine.minestars.R
import kotlinx.coroutines.launch
import cn.mine.minestars.Screen
import cn.mine.minestars.data.db.dao.QuickMessageDAO
import cn.mine.minestars.data.model.QuickMessage
import cn.mine.minestars.ui.components.ai.ExtensionEmptyState
import cn.mine.minestars.ui.components.ai.QuickMessagesContent
import cn.mine.minestars.ui.components.ai.SkillsContent
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.theme.CustomColors
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun AssistantExtensionsPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(id) })
    val quickMessageDAO: QuickMessageDAO = koinInject()
    val quickMessageEntities by quickMessageDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    val quickMessages = remember(quickMessageEntities) {
        quickMessageEntities.map { QuickMessage(id = kotlin.uuid.Uuid.parse(it.id), title = it.title, content = it.content) }
    }
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val skills by vm.skills.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 2 }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.assistant_extensions_page_title)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SecondaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(R.string.assistant_extensions_page_tab_quick_messages)) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(R.string.assistant_extensions_page_tab_skills)) }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                when (page) {
                    0 -> {
                        if (quickMessages.isEmpty()) {
                            ExtensionEmptyState(
                                message = stringResource(R.string.assistant_extensions_page_empty_quick_messages),
                                buttonText = stringResource(R.string.assistant_extensions_page_goto_extensions),
                                onAction = { navController.navigate(Screen.QuickMessages) },
                            )
                        } else {
                            Column {
                                QuickMessagesContent(
                                    modifier = Modifier.weight(1f),
                                    quickMessages = quickMessages,
                                    selectedIds = assistant.quickMessageIds,
                                    onToggle = { quickMessageId, checked ->
                                        val newIds = if (checked) assistant.quickMessageIds + quickMessageId
                                        else assistant.quickMessageIds - quickMessageId
                                        vm.update(assistant.copy(quickMessageIds = newIds))
                                    },
                                )
                                TextButton(
                                    onClick = { navController.navigate(Screen.QuickMessages) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.assistant_extensions_page_goto_extensions))
                                }
                            }
                        }
                    }

                    1 -> {
                        if (skills.isEmpty()) {
                            ExtensionEmptyState(
                                message = stringResource(R.string.assistant_extensions_page_empty_skills),
                                buttonText = stringResource(R.string.assistant_extensions_page_goto_extensions),
                                onAction = { navController.navigate(Screen.Skills) },
                            )
                        } else {
                            Column {
                                SkillsContent(
                                    modifier = Modifier.weight(1f),
                                    skills = skills,
                                    enabledSkills = assistant.enabledSkills,
                                    onToggle = { name, checked ->
                                        val newSkills = if (checked) assistant.enabledSkills + name
                                        else assistant.enabledSkills - name
                                        vm.update(assistant.copy(enabledSkills = newSkills))
                                    },
                                )
                                TextButton(
                                    onClick = { navController.navigate(Screen.Skills) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.assistant_extensions_page_goto_extensions))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
