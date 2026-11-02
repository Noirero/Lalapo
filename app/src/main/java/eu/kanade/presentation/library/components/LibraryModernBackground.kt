package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.theme.LocalModernArtworkIdentity
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernVisualPalette
import eu.kanade.presentation.theme.modernIdentityMotif
import eu.kanade.presentation.theme.modernWallpaperResource

/**
 * Built-in Pustaka artwork is owned by the Home shell so it can continue behind NavigationSuite.
 * This modifier only retains the existing procedural fallback for non-authored identities.
 */
@Composable
internal fun Modifier.modernLibraryBackground(): Modifier {
    val palette = LocalModernVisualPalette.current
    if (!palette.isModern) return this

    val identity = LocalModernArtworkIdentity.current
    val resource = modernWallpaperResource(isModern = true, identity = identity)
    if (resource != null) {
        // Built-in sharp artwork is owned by HomeScreen so it remains continuous behind navigation.
        return this
    }

    val stops = remember(palette) { modernLibraryBackgroundStops(palette) } ?: return this
    val brush = remember(stops) { Brush.verticalGradient(stops) }
    return background(brush).then(
        if (identity != null) Modifier.modernIdentityMotif(identity, palette) else Modifier,
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
