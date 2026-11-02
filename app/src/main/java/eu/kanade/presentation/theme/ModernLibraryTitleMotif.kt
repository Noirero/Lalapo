package eu.kanade.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Small authored motifs beside the Pustaka title (not application branding).
 * Ratios are based on the six owner references; none are drawn for Classic or Custom.
 */
@Composable
internal fun ModernLibraryTitleMotif(
    identity: ModernArtworkIdentity,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val width = when (identity) {
        ModernArtworkIdentity.SAKURA_BLOSSOM -> 39.dp
        ModernArtworkIdentity.EMERALD_FOLIAGE -> 39.dp
        ModernArtworkIdentity.CYAN_WATER_SKY -> 53.dp
        else -> 46.dp
    }
    Canvas(modifier = modifier.size(width = width, height = 42.dp)) {
        val w = size.width
        val h = size.height
        when (identity) {
            ModernArtworkIdentity.LALAPO_AURORA,
            ModernArtworkIdentity.VIOLET_MOON_AURORA,
            ModernArtworkIdentity.AMBER_MAPLE_SUNSET,
            -> {
                val count = if (identity == ModernArtworkIdentity.VIOLET_MOON_AURORA) 3 else 2
                val spacing = w / (count + 0.5f)
                repeat(count) { i ->
                    val x = spacing * (i + 0.45f)
                    val start = Offset(x, h * 0.90f)
                    val end = Offset(x + spacing * 0.55f, h * 0.10f)
                    drawLine(color.copy(alpha = 0.35f), start, end, strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color, start, end, strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            ModernArtworkIdentity.SAKURA_BLOSSOM -> {
                val center = Offset(w / 2f, h / 2f)
                val r = minOf(w, h) * 0.17f
                repeat(5) { i ->
                    val a = 2 * PI * i / 5 - PI / 2
                    drawCircle(
                        color = color,
                        radius = r,
                        center = Offset(
                            center.x + cos(a).toFloat() * r * 1.25f,
                            center.y + sin(a).toFloat() * r * 1.25f,
                        ),
                    )
                }
                drawCircle(Color(0xFFFFE0F0), radius = r * 0.45f, center = center)
            }
            ModernArtworkIdentity.CYAN_WATER_SKY -> {
                val wave = Path().apply {
                    moveTo(w * 0.04f, h * 0.57f)
                    cubicTo(w * 0.21f, h * 0.20f, w * 0.33f, h * 0.84f, w * 0.51f, h * 0.49f)
                    cubicTo(w * 0.70f, h * 0.16f, w * 0.83f, h * 0.72f, w * 0.98f, h * 0.34f)
                }
                drawPath(wave, color.copy(alpha = 0.40f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
                drawPath(wave, color, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            }
            ModernArtworkIdentity.EMERALD_FOLIAGE -> {
                val leaf = Path().apply {
                    moveTo(w * 0.12f, h * 0.78f)
                    quadraticTo(w * 0.25f, h * 0.07f, w * 0.96f, h * 0.11f)
                    quadraticTo(w * 0.89f, h * 0.78f, w * 0.12f, h * 0.78f)
                    close()
                }
                drawPath(leaf, color)
                drawLine(
                    Color(0xFF25591C),
                    Offset(w * 0.08f, h * 0.90f),
                    Offset(w * 0.83f, h * 0.23f),
                    strokeWidth = 1.8.dp.toPx(),
                )
            }
            ModernArtworkIdentity.CUSTOM_WAVES -> Unit
        }
    }
}
