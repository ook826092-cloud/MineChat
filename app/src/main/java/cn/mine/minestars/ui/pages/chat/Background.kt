package cn.mine.minestars.ui.pages.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.map
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.db.dao.AssistantDAO
import cn.mine.minestars.data.db.toModel
import cn.mine.minestars.data.model.Assistant
import org.koin.compose.koinInject

@Composable
fun AssistantBackground(setting: Settings, modifier: Modifier) {
    val assistantDao: AssistantDAO = koinInject()
    val assistant by assistantDao.getByIdFlow(setting.assistantId.toString())
        .map { it?.toModel() ?: Assistant() }
        .collectAsStateWithLifecycle(Assistant())
    if (assistant.useGradientBackground) {
        MeshGradientBackground(modifier = modifier)
        return
    }

    if (assistant.background != null) {
        val backgroundColor = MaterialTheme.colorScheme.background
        val backgroundOpacity = assistant.backgroundOpacity.coerceIn(0f, 1f)
        Box(modifier = modifier) {
            AsyncImage(
                model = assistant.background,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(backgroundOpacity)
            )

            // 全屏渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor.copy(alpha = 0.2f),
                                backgroundColor.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
        }
    }
}
