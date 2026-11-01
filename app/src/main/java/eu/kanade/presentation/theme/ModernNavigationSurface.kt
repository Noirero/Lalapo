package eu.kanade.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
/**
 * Built-in Modern can opt into a transparent Material container while its outer glass shell owns
 * the visible surface. Other Modern identities retain the previous semantic container.
 * Null means retain the pre-existing Classic NavigationSuite colors unchanged.
 */
internal fun modernNavigationContainerColor(
    scheme: ColorScheme,
    isModern: Boolean,
    useGlass: Boolean = false,
): Color? = when {
    !isModern -> null
    useGlass -> scheme.surfaceContainerHigh.copy(alpha = 0f)
    else -> scheme.surfaceContainerHigh
}
