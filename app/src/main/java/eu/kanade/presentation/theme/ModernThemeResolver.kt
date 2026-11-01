package eu.kanade.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import eu.kanade.presentation.theme.colorscheme.BaseColorScheme

/**
 * The Classic callback is invoked only for CLASSIC. Root MaterialExpressiveTheme receives
 * exactly this selected ColorScheme, never a second global Material or semantic color engine.
 */
internal fun resolveDesignStyleColorScheme(
    designStyle: DesignStyle,
    modernThemePreset: ModernThemePreset,
    isDark: Boolean,
    isAmoled: Boolean,
    classicResolver: () -> ColorScheme,
): ColorScheme {
    if (designStyle == DesignStyle.CLASSIC) return classicResolver()
    // Unauthored identities cannot accidentally acquire placeholder production colors.
    val definition = ModernThemeDefinitions.find(modernThemePreset) ?: ModernThemeDefinitions.Lalapo
    return ModernThemeResolver.resolve(definition, isDark, isAmoled)
}

/** An authored, deterministic M3 scheme generated from the canonical Modern identity. */
internal object ModernThemeResolver {
    private val lalapoScheme = object : BaseColorScheme() {
        override val lightScheme = buildScheme(ModernThemeDefinitions.Lalapo, dark = false)
        override val darkScheme = buildScheme(ModernThemeDefinitions.Lalapo, dark = true)
    }

    fun resolve(
        definition: ModernThemeDefinition,
        isDark: Boolean,
        isAmoled: Boolean,
    ): ColorScheme {
        require(definition.preset == ModernThemePreset.LALAPO && definition == ModernThemeDefinitions.Lalapo) {
            "The selected Modern preset does not yet have an authored ColorScheme"
        }
        return lalapoScheme.getColorScheme(
            isDark = isDark,
            isAmoled = isAmoled,
            overrideDarkSurfaceContainers = true,
        )
    }
}

/**
 * Compose's stable ColorScheme constructors are used rather than assuming an unverified
 * multi-seed API in the pinned MaterialKolor dependency.
 */
private fun buildScheme(definition: ModernThemeDefinition, dark: Boolean): ColorScheme {
    val ink = if (dark) Color(0xFF101223) else Color.White
    val primary = accessibleAccent(definition.primarySeed, ink, dark)
    val secondary = accessibleAccent(definition.secondarySeed, ink, dark)
    val tertiary = accessibleAccent(definition.tertiarySeed, ink, dark)
    val containerContent = if (dark) Color(0xFFF5F4FF) else Color(0xFF151529)
    val primaryContainer = container(definition.primarySeed, dark)
    val secondaryContainer = container(definition.secondarySeed, dark)
    val tertiaryContainer = container(definition.tertiarySeed, dark)

    return if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = ink,
            primaryContainer = primaryContainer,
            onPrimaryContainer = containerContent,
            secondary = secondary,
            onSecondary = ink,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = containerContent,
            tertiary = tertiary,
            onTertiary = ink,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = containerContent,
            background = Color(0xFF131527),
            onBackground = Color(0xFFF4F3FF),
            surface = Color(0xFF131527),
            onSurface = Color(0xFFF4F3FF),
            surfaceVariant = Color(0xFF30324A),
            onSurfaceVariant = Color(0xFFE1E1F3),
            surfaceContainerLowest = Color(0xFF0E1020),
            surfaceContainerLow = Color(0xFF191B30),
            surfaceContainer = Color(0xFF202239),
            surfaceContainerHigh = Color(0xFF292B43),
            surfaceContainerHighest = Color(0xFF34364E),
            outline = Color(0xFFA8AAC8),
            outlineVariant = Color(0xFF60637D),
            inverseSurface = Color(0xFFF4F3FF),
            inverseOnSurface = Color(0xFF171829),
            inversePrimary = accessibleAccent(definition.primarySeed, Color.White, dark = false),
            surfaceTint = primary,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = ink,
            primaryContainer = primaryContainer,
            onPrimaryContainer = containerContent,
            secondary = secondary,
            onSecondary = ink,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = containerContent,
            tertiary = tertiary,
            onTertiary = ink,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = containerContent,
            background = Color(0xFFF4F3FF),
            onBackground = Color(0xFF171829),
            surface = Color(0xFFF4F3FF),
            onSurface = Color(0xFF171829),
            surfaceVariant = Color(0xFFE5E5FA),
            onSurfaceVariant = Color(0xFF393A55),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF8F7FF),
            surfaceContainer = Color(0xFFEDEDFE),
            surfaceContainerHigh = Color(0xFFE4E5FA),
            surfaceContainerHighest = Color(0xFFDBDCF5),
            outline = Color(0xFF71728D),
            outlineVariant = Color(0xFFC5C6E1),
            inverseSurface = Color(0xFF282A41),
            inverseOnSurface = Color(0xFFF4F3FF),
            inversePrimary = accessibleAccent(definition.primarySeed, Color(0xFF101223), dark = true),
            surfaceTint = primary,
        )
    }
}

private fun accessibleAccent(seed: Color, foreground: Color, dark: Boolean): Color {
    var candidate = seed
    repeat(20) {
        if (contrast(foreground, candidate) >= 4.5f) return candidate
        candidate = lerp(candidate, if (dark) Color.White else Color.Black, 0.18f)
    }
    return if (dark) Color.White else Color.Black
}

private fun container(seed: Color, dark: Boolean): Color =
    lerp(seed, if (dark) Color.Black else Color.White, if (dark) 0.72f else 0.86f)

private fun contrast(a: Color, b: Color): Float {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
