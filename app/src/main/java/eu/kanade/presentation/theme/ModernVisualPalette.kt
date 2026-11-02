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

@Immutable
internal data class ModernGlassPalette(
    val surface: Color,
    val surfaceStrong: Color,
    val railSurface: Color,
    val border: Color,
    val borderStrong: Color,
    val selectedSurface: Color,
    val selectedBorder: Color,
    val innerHighlight: Color,
    val glow: Color,
    val selectedGlow: Color,
    val cardGlow: Color,
    val content: Color,
    val contentMuted: Color,
)

/**
 * Miyorare-derived luminous glass roles resolved from Lalapo's canonical Material scheme.
 * This is presentation-only: preset identity and semantic Material colors remain owned by the
 * existing Modern theme resolver.
 */
internal fun ModernVisualPalette.resolveGlassPalette(colorScheme: ColorScheme): ModernGlassPalette {
    val isLight = colorScheme.background.luminance() >= 0.5f

    // Port the approved Normal-Favourites role strengths while keeping Lalapo's semantic scheme
    // as the hue/source authority.
    val luminousPrimary = luminousThemeColor(colorScheme.primary)
    val luminousAccent = luminousThemeColor(
        color = lerp(colorScheme.primary, colorScheme.secondary, 0.25f),
        saturationFloor = 0.86f,
        valueFloor = 1.00f,
        valueBoost = 0.14f,
    )
    val hotEdge = lerp(luminousAccent, Color.White, 0.42f)
    val selectedEdge = lerp(luminousAccent, Color.White, 0.62f)
    val glassBase = lerp(Color.Black, luminousPrimary, 0.30f)
    val strongBase = lerp(Color.Black, luminousPrimary, 0.40f)
    val railBase = lerp(Color.Black, luminousAccent, 0.34f)
    val selectedBase = lerp(luminousAccent, Color.White, 0.22f)
    val content = if (isLight) colorScheme.onSurface else Color.White

    return ModernGlassPalette(
        surface = glassBase.copy(alpha = 84f / 255f),
        surfaceStrong = strongBase.copy(alpha = 108f / 255f),
        railSurface = railBase.copy(alpha = 106f / 255f),
        border = luminousAccent.copy(alpha = 208f / 255f),
        borderStrong = hotEdge.copy(alpha = 246f / 255f),
        selectedSurface = selectedBase.copy(alpha = 176f / 255f),
        selectedBorder = selectedEdge,
        innerHighlight = selectedEdge.copy(alpha = 250f / 255f),
        glow = luminousAccent.copy(alpha = 176f / 255f),
        selectedGlow = luminousAccent.copy(alpha = 228f / 255f),
        cardGlow = luminousAccent.copy(alpha = 104f / 255f),
        content = content,
        contentMuted = content.copy(alpha = if (isLight) 0.82f else 234f / 255f),
    )
}
private fun luminousThemeColor(
    color: Color,
    saturationFloor: Float = 0.74f,
    valueFloor: Float = 0.94f,
    valueBoost: Float = 0.12f,
): Color {
    val red = color.red.coerceIn(0f, 1f)
    val green = color.green.coerceIn(0f, 1f)
    val blue = color.blue.coerceIn(0f, 1f)
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    var saturation = if (max == 0f) 0f else delta / max
    if (saturation > NEUTRAL_SATURATION_THRESHOLD) {
        saturation = maxOf(saturation, saturationFloor).coerceIn(0f, 1f)
    }
    val value = maxOf(valueFloor, max + valueBoost).coerceIn(0f, 1f)
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    val chroma = value * saturation
    val x = chroma * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = value - chroma
    val (r1, g1, b1) = when (hue) {
        in 0f..<60f -> Triple(chroma, x, 0f)
        in 60f..<120f -> Triple(x, chroma, 0f)
        in 120f..<180f -> Triple(0f, chroma, x)
        in 180f..<240f -> Triple(0f, x, chroma)
        in 240f..<300f -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = color.alpha,
    )
}

private const val NEUTRAL_SATURATION_THRESHOLD = 0.08f

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
