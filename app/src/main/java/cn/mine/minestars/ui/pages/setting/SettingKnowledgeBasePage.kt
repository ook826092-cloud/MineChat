package cn.mine.minestars.ui.pages.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Database01
import me.rerere.hugeicons.stroke.Delete02
import me.rerere.hugeicons.stroke.ArrowRight01
import cn.mine.minestars.R
import cn.mine.minestars.Screen
import cn.mine.minestars.data.db.dao.KnowledgeBaseDAO
import cn.mine.minestars.data.db.entity.KnowledgeBaseEntity
import cn.mine.minestars.rag.KnowledgeBase
import cn.mine.minestars.ui.components.nav.BackButton
import cn.mine.minestars.ui.components.ui.CardGroup
import cn.mine.minestars.ui.context.LocalNavController
import cn.mine.minestars.ui.theme.CustomColors
import cn.mine.minestars.utils.JsonInstant
import cn.mine.minestars.utils.plus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.koin.compose.koinInject

@Composable
fun SettingKnowledgeBasePage(
    knowledgeBaseDAO: KnowledgeBaseDAO = koinInject(),
) {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val knowledgeBaseEntities by knowledgeBaseDAO.getAllFlow().collectAsStateWithLifecycle(emptyList())
    val knowledgeBases = remember(knowledgeBaseEntities) {
        knowledgeBaseEntities.map { it.toKnowledgeBase() }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("知识库") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
                actions = {
                    IconButton(onClick = {
                        val newKb = KnowledgeBase(name = "新建知识库")
                        scope.launch {
                            knowledgeBaseDAO.insert(newKb.toEntity())
                        }
                        navController.navigate(Screen.SettingKnowledgeBaseDetail(newKb.id.toString()))
                    }) {
                        Icon(HugeIcons.Add01, "新建")
                    }
                },
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        if (knowledgeBases.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无知识库，点击右上角 + 新建",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding + PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    CardGroup(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        title = { Text("所有知识库") },
                    ) {
                        knowledgeBases.forEachIndexed { index, kb ->
                            item(
                                onClick = {
                                    navController.navigate(Screen.SettingKnowledgeBaseDetail(kb.id.toString()))
                                },
                                leadingContent = {
                                    Icon(HugeIcons.Database01, null)
                                },
                                headlineContent = {
                                    Text(kb.name.ifBlank { "未命名知识库" })
                                },
                                supportingContent = {
                                    Text("${kb.files.size} 个文件 · topK=${kb.topK}")
                                },
                                trailingContent = {
                                    Icon(
                                        HugeIcons.ArrowRight01,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun KnowledgeBaseEntity.toKnowledgeBase(): KnowledgeBase {
    val files = runCatching {
        val configObj = JsonInstant.decodeFromString<kotlinx.serialization.json.JsonObject>(this.config)
        configObj["files"]?.jsonArray?.map { it.jsonPrimitive.content }
    }.getOrNull() ?: emptyList()
    return KnowledgeBase(
        id = kotlin.uuid.Uuid.parse(this.id),
        name = this.name,
        embeddingModelId = this.embeddingModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() },
        rerankModelId = this.rerankModelId?.let { runCatching { kotlin.uuid.Uuid.parse(it) }.getOrNull() },
        files = files,
        topK = this.topK,
        similarityThreshold = this.similarityThreshold,
        embeddingDimensions = this.embeddingDimensions,
    )
}

private fun KnowledgeBase.toEntity(): KnowledgeBaseEntity {
    val configObj = kotlinx.serialization.json.JsonObject(mapOf(
        "files" to JsonArray(this.files.map { JsonPrimitive(it) })
    ))
    return KnowledgeBaseEntity(
        id = this.id.toString(),
        name = this.name,
        embeddingModelId = this.embeddingModelId?.toString(),
        rerankModelId = this.rerankModelId?.toString(),
        topK = this.topK,
        similarityThreshold = this.similarityThreshold,
        embeddingDimensions = this.embeddingDimensions,
        config = JsonInstant.encodeToString(configObj),
    )
}
