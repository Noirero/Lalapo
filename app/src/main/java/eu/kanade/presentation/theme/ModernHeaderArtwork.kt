package eu.kanade.presentation.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import eu.kanade.tachiyomi.R

/**
 * A single project-authored illustration per Modern built-in.
 * Sources: Lalapo_Modern_Header_Master_Sources.zip, reduced to drawable-nodpi WebP.
 *
 * Null means no asset is eligible: Classic never decodes or draws Modern artwork.
 */
internal fun modernHeaderArtworkResource(
    isModern: Boolean,
    identity: ModernArtworkIdentity?,
): Int? = if (!isModern || identity == null) {
    null
} else {
    when (identity) {
        ModernArtworkIdentity.LALAPO_AURORA -> R.drawable.modern_header_lalapo
        ModernArtworkIdentity.SAKURA_BLOSSOM -> R.drawable.modern_header_sakura
        ModernArtworkIdentity.VIOLET_MOON_AURORA -> R.drawable.modern_header_violet
        ModernArtworkIdentity.CYAN_WATER_SKY -> R.drawable.modern_header_cyan
        ModernArtworkIdentity.EMERALD_FOLIAGE -> R.drawable.modern_header_emerald
        ModernArtworkIdentity.AMBER_MAPLE_SUNSET -> R.drawable.modern_header_amber
    }
}

/**
 * Decorates the existing Pustaka toolbar bounds only; adds no layout height or
 * interactive/accessible element. The title, search and category controls remain Compose.
 * The image is opaque RGB WebP and needs semantic scrims for readable foregrounds.
 */
@Composable
internal fun ModernHeaderArtwork(modifier: Modifier = Modifier) {
    val resource = modernHeaderArtworkResource(
        isModern = LocalModernVisualPalette.current.isModern,
        identity = LocalModernArtworkIdentity.current,
    ) ?: return

    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val isAmoled = isDark && scheme.background == Color.Black
    val scrimColor = if (isAmoled) Color.Black else scheme.background
    val horizontal = remember(scrimColor, isDark) {
        Brush.horizontalGradient(
            0f to scrimColor.copy(alpha = 0.87f),
            0.55f to scrimColor.copy(alpha = if (isDark) 0.42f else 0.48f),
            1f to scrimColor.copy(alpha = 0.22f),
        )
    }
    val vertical = remember(scrimColor, isAmoled) {
        Brush.verticalGradient(
            0f to scrimColor.copy(alpha = 0.16f),
            0.60f to scrimColor.copy(alpha = if (isAmoled) 0.68f else 0.52f),
            1f to scrimColor,
        )
    }

    Box(modifier = modifier.clipToBounds()) {
        Image(
            painter = painterResource(id = resource),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            alpha = if (isAmoled) 0.72f else 0.90f,
        )
        Box(modifier = Modifier.matchParentSize().background(horizontal))
        Box(modifier = Modifier.matchParentSize().background(vertical))
    }
}
