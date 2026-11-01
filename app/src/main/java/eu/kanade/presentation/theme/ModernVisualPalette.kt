package eu.kanade.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Semantic visual roles used by Lalapo's optional Modern presentation layer.
 *
 * Material [ColorScheme] remains the authority for ordinary semantic colors. This palette only
 * carries decorative roles that Material 3 does not model directly.
 */
@Immutable
data class ModernVisualPalette(
    val isModern: Boolean,
    val backgroundGradientStart: Color,
    val backgroundGradientMiddle: Color,
    val backgroundGradientEnd: Color,
    val surfaceGradientStart: Color,
    val surfaceGradientMiddle: Color,
    val surfaceGradientEnd: Color,
    val accentGradientStart: Color,
    val accentGradientMiddle: Color,
    val accentGradientEnd: Color,
    val activeGradientStart: Color,
    val activeGradientEnd: Color,
    val borderHighlight: Color,
    val glow: Color,
    val accentGradientContentColor: Color,
    val glassSurfaceAlpha: Float,
) {
    companion object {
        val Standard = ModernVisualPalette(
            isModern = false,
            backgroundGradientStart = Color.Unspecified,
            backgroundGradientMiddle = Color.Unspecified,
            backgroundGradientEnd = Color.Unspecified,
            surfaceGradientStart = Color.Unspecified,
            surfaceGradientMiddle = Color.Unspecified,
            surfaceGradientEnd = Color.Unspecified,
            accentGradientStart = Color.Unspecified,
            accentGradientMiddle = Color.Unspecified,
            accentGradientEnd = Color.Unspecified,
            activeGradientStart = Color.Unspecified,
            activeGradientEnd = Color.Unspecified,
            borderHighlight = Color.Transparent,
            glow = Color.Transparent,
            accentGradientContentColor = Color.Unspecified,
            glassSurfaceAlpha = 1f,
        )
    }
}

@Immutable
internal data class CinematicAccentPalette(
    val accentGradientStart: Color,
    val accentGradientMiddle: Color,
    val accentGradientEnd: Color,
    val activeGradientStart: Color,
    val activeGradientEnd: Color,
    val borderHighlight: Color,
    val glow: Color,
    val accentGradientContentColor: Color,
) {
    companion object {
        val Standard = CinematicAccentPalette(
            accentGradientStart = Color.Unspecified,
            accentGradientMiddle = Color.Unspecified,
            accentGradientEnd = Color.Unspecified,
            activeGradientStart = Color.Unspecified,
            activeGradientEnd = Color.Unspecified,
            borderHighlight = Color.Transparent,
            glow = Color.Transparent,
            accentGradientContentColor = Color.Unspecified,
        )
    }
}

internal val LocalModernVisualPalette = staticCompositionLocalOf { ModernVisualPalette.Standard }
internal val LocalCinematicAccentPalette = staticCompositionLocalOf<CinematicAccentPalette?> { null }

@Composable
internal fun ProvideCinematicAccentPalette(
    palette: CinematicAccentPalette?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalCinematicAccentPalette provides palette,
        content = content,
    )
}

internal fun ModernVisualPalette.resolveAccentPalette(
    cinematicAccentPalette: CinematicAccentPalette?,
): CinematicAccentPalette {
    if (!isModern) return CinematicAccentPalette.Standard
    return cinematicAccentPalette ?: CinematicAccentPalette(
        accentGradientStart = accentGradientStart,
        accentGradientMiddle = accentGradientMiddle,
        accentGradientEnd = accentGradientEnd,
        activeGradientStart = activeGradientStart,
        activeGradientEnd = activeGradientEnd,
        borderHighlight = borderHighlight,
        glow = glow,
        accentGradientContentColor = accentGradientContentColor,
    )
}

internal fun resolveModernVisualPalette(
    colorScheme: ColorScheme,
    enabled: Boolean,
    definition: ModernThemeDefinition? = null,
    isAmoled: Boolean = false,
): ModernVisualPalette {
    if (!enabled) return ModernVisualPalette.Standard

    val isLight = colorScheme.background.luminance() >= 0.5f
    val gradientStrength = BALANCED_GRADIENT_STRENGTH

    val accentContentColor = bestReadableContentColor(
        background = colorScheme.primary,
        preferred = colorScheme.onPrimary,
    )
    val accentGradientStart = colorScheme.primary
    val accentGradientMiddle = ensureBackgroundContrast(
        background = lerp(colorScheme.primary, colorScheme.secondary, 0.42f),
        foreground = accentContentColor,
        fallback = colorScheme.primary,
    )
    val accentGradientEnd = ensureBackgroundContrast(
        background = lerp(colorScheme.primary, colorScheme.tertiary, 0.34f),
        foreground = accentContentColor,
        fallback = colorScheme.primary,
    )

    val palette = ModernVisualPalette(
        isModern = true,
        backgroundGradientStart = ensureBackgroundContrast(
            background = lerp(
                colorScheme.background,
                colorScheme.primary,
                gradientStrength * 0.22f,
            ),
            foreground = colorScheme.onBackground,
            fallback = colorScheme.background,
        ),
        backgroundGradientMiddle = ensureBackgroundContrast(
            background = lerp(
                colorScheme.background,
                colorScheme.secondary,
                gradientStrength * 0.12f,
            ),
            foreground = colorScheme.onBackground,
            fallback = colorScheme.background,
        ),
        backgroundGradientEnd = ensureBackgroundContrast(
            background = lerp(
                colorScheme.background,
                colorScheme.tertiary,
                gradientStrength * 0.18f,
            ),
            foreground = colorScheme.onBackground,
            fallback = colorScheme.background,
        ),
        surfaceGradientStart = ensureBackgroundContrast(
            background = lerp(
                colorScheme.surfaceContainer,
                colorScheme.primary,
                gradientStrength * 0.28f,
            ),
            foreground = colorScheme.onSurface,
            fallback = colorScheme.surface,
        ),
        surfaceGradientMiddle = ensureBackgroundContrast(
            background = lerp(
                colorScheme.surfaceContainerHigh,
                colorScheme.tertiary,
                gradientStrength * 0.10f,
            ),
            foreground = colorScheme.onSurface,
            fallback = colorScheme.surface,
        ),
        surfaceGradientEnd = ensureBackgroundContrast(
            background = lerp(
                colorScheme.surfaceContainerHigh,
                colorScheme.secondary,
                gradientStrength * 0.32f,
            ),
            foreground = colorScheme.onSurface,
            fallback = colorScheme.surface,
        ),
        accentGradientStart = accentGradientStart,
        accentGradientMiddle = accentGradientMiddle,
        accentGradientEnd = accentGradientEnd,
        activeGradientStart = ensureBackgroundContrast(
            background = lerp(
                colorScheme.primary,
                colorScheme.secondary,
                gradientStrength * 0.42f,
            ),
            foreground = accentContentColor,
            fallback = colorScheme.primary,
        ),
        activeGradientEnd = ensureBackgroundContrast(
            background = lerp(
                colorScheme.primary,
                colorScheme.tertiary,
                0.12f + gradientStrength * 0.24f,
            ),
            foreground = accentContentColor,
            fallback = colorScheme.primary,
        ),
        borderHighlight = lerp(
            colorScheme.outline,
            definition?.secondarySeed ?: colorScheme.secondary,
            0.22f + gradientStrength * 0.26f,
        ).copy(alpha = BALANCED_BORDER_ALPHA),
        glow = lerp(
            colorScheme.primary,
            definition?.tertiarySeed ?: colorScheme.secondary,
            0.34f,
        ).copy(alpha = if (isLight) LIGHT_GLOW_ALPHA else BALANCED_GLOW_ALPHA),
        accentGradientContentColor = accentContentColor,
        glassSurfaceAlpha = if (isLight) LIGHT_GLASS_SURFACE_ALPHA else DARK_GLASS_SURFACE_ALPHA,
    )
    // A pure-black body is an invariant, not a user-adjustable decorative gradient.
    return if (isAmoled) {
        palette.copy(
            backgroundGradientStart = Color.Black,
            backgroundGradientMiddle = Color.Black,
            backgroundGradientEnd = Color.Black,
            glow = Color.Transparent,
        )
    } else {
        palette
    }
}

private fun bestReadableContentColor(
    background: Color,
    preferred: Color,
): Color {
    if (contrastRatio(preferred, background) >= MINIMUM_TEXT_CONTRAST) return preferred

    val dark = Color(0xFF0A0A10)
    return if (contrastRatio(Color.White, background) >= contrastRatio(dark, background)) {
        Color.White
    } else {
        dark
    }
}

private fun ensureBackgroundContrast(
    background: Color,
    foreground: Color,
    fallback: Color,
): Color {
    if (contrastRatio(foreground, background) >= MINIMUM_TEXT_CONTRAST) return background

    var candidate = background
    repeat(8) {
        candidate = lerp(candidate, fallback, 0.18f)
        if (contrastRatio(foreground, candidate) >= MINIMUM_TEXT_CONTRAST) return candidate
    }
    return fallback
}

private fun contrastRatio(
    first: Color,
    second: Color,
): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

private const val BALANCED_GRADIENT_STRENGTH = 0.24f
private const val BALANCED_BORDER_ALPHA = 0.34f
private const val BALANCED_GLOW_ALPHA = 0.16f
private const val LIGHT_GLOW_ALPHA = 0.10f
private const val LIGHT_GLASS_SURFACE_ALPHA = 0.84f
private const val DARK_GLASS_SURFACE_ALPHA = 0.92f
private const val MINIMUM_TEXT_CONTRAST = 4.5f
