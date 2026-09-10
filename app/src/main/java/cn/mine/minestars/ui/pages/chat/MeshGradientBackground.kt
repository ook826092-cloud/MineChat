package cn.mine.minestars.ui.pages.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeshGradientBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mesh_gradient")

    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = if (darkTheme) DarkGradientColors else LightGradientColors

    val blob1Offset = animateBlobOffset(transition, 0, periodMs = 14_000)
    val blob2Offset = animateBlobOffset(transition, 1, periodMs = 17_000)
    val blob3Offset = animateBlobOffset(transition, 2, periodMs = 20_000)

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasSize = size
        val maxDim = maxOf(canvasSize.width, canvasSize.height)

        // Background base
        drawRect(color = colors.base)

        // Blob 1
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.blob1,
                    colors.blob1.copy(alpha = 0.0f),
                ),
                center = Offset(
                    canvasSize.width * blob1Offset.x,
                    canvasSize.height * blob1Offset.y,
                ),
                radius = maxDim * 0.7f,
            ),
        )

        // Blob 2
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.blob2,
                    colors.blob2.copy(alpha = 0.0f),
                ),
                center = Offset(
                    canvasSize.width * blob2Offset.x,
                    canvasSize.height * blob2Offset.y,
                ),
                radius = maxDim * 0.6f,
            ),
        )

        // Blob 3
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.blob3,
                    colors.blob3.copy(alpha = 0.0f),
                ),
                center = Offset(
                    canvasSize.width * blob3Offset.x,
                    canvasSize.height * blob3Offset.y,
                ),
                radius = maxDim * 0.5f,
            ),
        )
    }
}

private data class GradientColors(
    val base: Color,
    val blob1: Color,
    val blob2: Color,
    val blob3: Color,
)

private val LightGradientColors = GradientColors(
    base = Color(0xFFF5E6FF),
    blob1 = Color(0xFFD4A5FF),
    blob2 = Color(0xFFA5D6FF),
    blob3 = Color(0xFFFFC8A5),
)

private val DarkGradientColors = GradientColors(
    base = Color(0xFF1A0A2E),
    blob1 = Color(0xFF4A1A7A),
    blob2 = Color(0xFF1A3A6A),
    blob3 = Color(0xFF6A2A1A),
)

@Composable
private fun animateBlobOffset(
    transition: InfiniteTransition,
    index: Int,
    periodMs: Int,
): Offset {
    val phaseOffset = index * 2.094f // 120 degrees in radians

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = periodMs,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob_${index}_progress",
    )

    // Map progress through easing for more natural movement
    val eased = FastOutSlowInEasing.transform(progress)

    // Circular orbit
    val angle = eased * 2f * Math.PI.toFloat() + phaseOffset
    val radius = 0.25f + 0.15f * sin(eased * Math.PI.toFloat())

    return Offset(
        x = 0.5f + radius * cos(angle),
        y = 0.5f + radius * sin(angle * 0.7f),
    )
}
