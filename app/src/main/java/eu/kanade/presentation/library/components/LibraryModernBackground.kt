package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernVisualPalette

/** A single, static decoration behind the existing Pustaka layout and pager. */
@Composable
internal fun Modifier.modernLibraryBackground(): Modifier {
    val palette = LocalModernVisualPalette.current
    val stops = remember(palette) { modernLibraryBackgroundStops(palette) } ?: return this
    val brush = remember(stops) { Brush.verticalGradient(stops) }
    return background(brush)
}

/**
 * No Classic decoration. Modern AMOLED receives black stops from the canonical
 * palette and therefore cannot tint the body behind the scrolling content.
 */
internal fun modernLibraryBackgroundStops(palette: ModernVisualPalette): List<Color>? {
    if (!palette.isModern) return null
    return listOf(
        palette.backgroundGradientStart,
        palette.backgroundGradientMiddle,
        palette.backgroundGradientEnd,
    )
}
