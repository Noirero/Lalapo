package eu.kanade.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Material's navigation indicator already uses the selected theme's semantic roles.
 * Only the shared NavigationBar/NavigationRail container needs a scoped Modern tone.
 * Null means retain the pre-existing Classic NavigationSuite colors unchanged.
 */
internal fun modernNavigationContainerColor(
    scheme: ColorScheme,
    isModern: Boolean,
): Color? = if (isModern) scheme.surfaceContainerHigh else null
