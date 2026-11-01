package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import eu.kanade.presentation.theme.LocalModernArtworkIdentity
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernVisualPalette
import eu.kanade.presentation.theme.modernIdentityMotif
import eu.kanade.presentation.theme.modernWallpaperResource

/**
 * Modern Pustaka owns the sharp authored portrait. Other destinations see the shared diffused
 * derivative from the root theme; keeping this override here reproduces Miyorare's sharp-library /
 * blurred-everywhere-else split without creating another state or theme engine.
 */
@Composable
internal fun Modifier.modernLibraryBackground(): Modifier {
    val palette = LocalModernVisualPalette.current
    if (!palette.isModern) return this

    val identity = LocalModernArtworkIdentity.current
    val resource = modernWallpaperResource(isModern = true, identity = identity)
    if (resource == null) {
        // Custom intentionally keeps Lalapo's existing lightweight procedural presentation.
        val stops = remember(palette) { modernLibraryBackgroundStops(palette) } ?: return this
        val brush = remember(stops) { Brush.verticalGradient(stops) }
        return background(brush).then(
            if (identity != null) Modifier.modernIdentityMotif(identity, palette) else Modifier,
        )
    }

    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() >= 0.5f
    val isAmoled = scheme.background.red == 0f &&
        scheme.background.green == 0f && scheme.background.blue == 0f
    val washColor = if (isAmoled) Color.Black else scheme.background.copy(alpha = 1f)
    val washStops = remember(washColor, isLight, isAmoled) {
        when {
            isAmoled -> listOf(
                Color.Transparent,
                washColor.copy(alpha = 0.24f),
                washColor.copy(alpha = 0.70f),
            )
            isLight -> listOf(
                washColor.copy(alpha = 0.04f),
                washColor.copy(alpha = 0.11f),
                washColor.copy(alpha = 0.22f),
            )
            else -> listOf(
                washColor.copy(alpha = 0.08f),
                washColor.copy(alpha = 0.18f),
                washColor.copy(alpha = 0.34f),
            )
        }
    }

    return paint(
        painter = painterResource(resource),
        alignment = Alignment.TopCenter,
        contentScale = ContentScale.FillWidth,
        sizeToIntrinsics = false,
    ).background(
        brush = Brush.verticalGradient(washStops),
    )
}

/**
 * Retained for Custom and deterministic palette tests. Built-in Pustaka now renders authored
 * portraits and therefore does not use these stops as its primary visual source.
 */
internal fun modernLibraryBackgroundStops(palette: ModernVisualPalette): List<Color>? {
    if (!palette.isModern) return null
    return listOf(
        palette.backgroundGradientStart,
        palette.backgroundGradientMiddle,
        palette.backgroundGradientEnd,
    )
}
