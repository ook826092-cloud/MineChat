package cn.mine.minestars.feature.character

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import cn.mine.minestars.R
import cn.mine.minestars.data.db.entity.CharacterCardEntity
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun CharacterCardListPage(vm: CharacterCardVM = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val cards by vm.characterCards.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()

    // File picker for PNG/JSON/WebP character cards
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.importCharacterCard(context.contentResolver, uri)
        }
    }

    // Handle import events
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is CharacterCardEvent.ImportSuccess -> {
                    snackbarHostState.showSnackbar(
                        "已导入 '${event.result.characterName}'" +
                                "（${event.result.worldBookCount} 个世界书，${event.result.regexCount} 个正则）"
                    )
                }
                is CharacterCardEvent.ImportError -> {
                    snackbarHostState.showSnackbar("导入失败: ${event.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = { BackButton() },
                title = { Text("角色卡") },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
                actions = {
                    IconButton(
                        onClick = {
                            importLauncher.launch(arrayOf(
                                "image/png",
                                "image/webp",
                                "application/json",
                                "text/plain",
                                "*/*"
                            ))
                        }
                    ) {
                        Icon(HugeIcons.Add01, "导入角色卡")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (importing) "导入中..." else "暂无角色卡\n点击右上角 + 导入",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(cards, key = { it.id }) { card ->
                CharacterCardListItem(
                    card = card,
                    onDelete = { vm.deleteCard(card.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CharacterCardListItem(
    card: CharacterCardEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { /* TODO: navigate to detail */ },
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = card.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = card.sourceFileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
