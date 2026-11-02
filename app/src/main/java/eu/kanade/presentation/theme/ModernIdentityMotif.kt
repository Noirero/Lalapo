package eu.kanade.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** A scoped identity hook; Classic has no artwork or decorative Modern identity. */
internal val LocalModernArtworkIdentity = staticCompositionLocalOf<ModernArtworkIdentity?> { null }

/**
 * Lightweight, static motifs restricted to the existing Pustaka header atmosphere.
 * No bitmap, runtime shaders, per-item effects, continuous animations, or extra theme.
 */
internal fun Modifier.modernIdentityMotif(
    identity: ModernArtworkIdentity,
    palette: ModernVisualPalette,
): Modifier = drawBehind {
    val anchor = Offset(
        x = size.width * 0.87f,
        y = minOf(size.height * 0.055f, 160.dp.toPx()),
    )
    val radius = 33.dp.toPx()
    val color = palette.borderHighlight.copy(alpha = 0.20f)
    val faint = palette.surfaceGradientEnd.copy(alpha = 0.13f)

    when (identity) {
        ModernArtworkIdentity.LALAPO_AURORA -> {
            drawCircle(color = faint, radius = radius * 2.2f, center = anchor)
            drawCircle(color = color, radius = radius * 1.35f, center = anchor, style = Stroke(1.dp.toPx()))
        }
        ModernArtworkIdentity.SAKURA_BLOSSOM -> {
            for (index in 0 until 5) {
                val angle = index * (Math.PI * 2.0 / 5.0)
                val center = Offset(
                    x = anchor.x + kotlin.math.cos(angle).toFloat() * radius * 0.75f,
                    y = anchor.y + kotlin.math.sin(angle).toFloat() * radius * 0.75f,
                )
                drawCircle(color = color, radius = radius * 0.52f, center = center)
            }
            drawCircle(color = faint, radius = radius * 0.33f, center = anchor)
        }
        ModernArtworkIdentity.VIOLET_MOON_AURORA -> {
            drawCircle(color = color, radius = radius * 1.2f, center = anchor, style = Stroke(2.dp.toPx()))
            drawCircle(
                color = faint,
                radius = radius * 1.85f,
                center = anchor,
                style = Stroke(1.dp.toPx()),
            )
        }
        ModernArtworkIdentity.CYAN_WATER_SKY -> {
            for (index in 1..3) {
                drawCircle(
                    color = color.copy(alpha = 0.10f),
                    radius = radius * (0.75f + index * 0.5f),
                    center = anchor,
                    style = Stroke(1.dp.toPx()),
                )
            }
        }
        ModernArtworkIdentity.EMERALD_FOLIAGE -> {
            drawOval(
                color = color,
                topLeft = Offset(anchor.x - radius * 1.1f, anchor.y - radius * 0.6f),
                size = Size(radius * 1.5f, radius * 0.72f),
            )
            drawOval(
                color = faint,
                topLeft = Offset(anchor.x - radius * 0.1f, anchor.y - radius * 0.1f),
                size = Size(radius * 1.1f, radius * 1.5f),
            )
        }
        ModernArtworkIdentity.CUSTOM_WAVES -> {
            // Static abstract concentric lights: confined to the top, inexpensive to draw.
            repeat(3) { index ->
                drawCircle(
                    color = color.copy(alpha = 0.10f),
                    radius = radius * (1.5f + index * 0.9f),
                    center = anchor,
                    style = Stroke(1.dp.toPx()),
                )
            }
        }
        ModernArtworkIdentity.AMBER_MAPLE_SUNSET -> {
            drawCircle(color = faint, radius = radius * 1.25f, center = anchor)
            for (index in 0 until 6) {
                val angle = index * (Math.PI / 3.0)
                val direction = Offset(
                    kotlin.math.cos(angle).toFloat(),
                    kotlin.math.sin(angle).toFloat(),
                )
                drawLine(
                    color = color,
                    start = Offset(anchor.x + direction.x * radius * 1.35f, anchor.y + direction.y * radius * 1.35f),
                    end = Offset(anchor.x + direction.x * radius * 1.85f, anchor.y + direction.y * radius * 1.85f),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }
    }
}
