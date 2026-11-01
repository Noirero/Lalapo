package eu.kanade.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import eu.kanade.domain.ui.model.ModernThemePreset

/** Authored Modern identity; Material and decorative roles must use this same definition. */
@Immutable
internal data class ModernThemeDefinition(
    val preset: ModernThemePreset,
    val primarySeed: Color,
    val secondarySeed: Color,
    val tertiarySeed: Color,
    val decorativeIdentity: ModernDecorativeIdentity,
    val artworkIdentity: ModernArtworkIdentity,
)

internal enum class ModernDecorativeIdentity {
    LALAPO_AURORA,
    SAKURA_BLOSSOM,
    VIOLET_MOON_AURORA,
    CYAN_WATER_SKY,
    EMERALD_FOLIAGE,
    AMBER_MAPLE_SUNSET,
    CUSTOM_WAVES,
}

internal enum class ModernArtworkIdentity {
    LALAPO_AURORA,
    SAKURA_BLOSSOM,
    VIOLET_MOON_AURORA,
    CYAN_WATER_SKY,
    EMERALD_FOLIAGE,
    AMBER_MAPLE_SUNSET,
    CUSTOM_WAVES,
}

internal object ModernThemeDefinitions {
    // Authored from Lalapo's approved blue/cyan/rose Modern visual direction.
    val Lalapo = ModernThemeDefinition(
        preset = ModernThemePreset.LALAPO,
        primarySeed = Color(0xFF485CE0),
        secondarySeed = Color(0xFF04BBD4),
        tertiarySeed = Color(0xFFE84AAC),
        decorativeIdentity = ModernDecorativeIdentity.LALAPO_AURORA,
        artworkIdentity = ModernArtworkIdentity.LALAPO_AURORA,
    )

    // Seed samples are drawn from the selected accents, header and atmospheric regions
    // of the owner-approved Theme_Modern_Lalapo_<Preset>.png visual references.
    // Production colors are frozen here; no Classic AppTheme colors or runtime sampling.
    val Sakura = ModernThemeDefinition(
        preset = ModernThemePreset.SAKURA,
        primarySeed = Color(0xFFC6436A),
        secondarySeed = Color(0xFFF7A092),
        tertiarySeed = Color(0xFFFD62A2),
        decorativeIdentity = ModernDecorativeIdentity.SAKURA_BLOSSOM,
        artworkIdentity = ModernArtworkIdentity.SAKURA_BLOSSOM,
    )

    val Violet = ModernThemeDefinition(
        preset = ModernThemePreset.VIOLET,
        primarySeed = Color(0xFF6F25C4),
        secondarySeed = Color(0xFFA164E0),
        tertiarySeed = Color(0xFFD763F2),
        decorativeIdentity = ModernDecorativeIdentity.VIOLET_MOON_AURORA,
        artworkIdentity = ModernArtworkIdentity.VIOLET_MOON_AURORA,
    )

    val Cyan = ModernThemeDefinition(
        preset = ModernThemePreset.CYAN,
        primarySeed = Color(0xFF048DA8),
        secondarySeed = Color(0xFF6CCEE7),
        tertiarySeed = Color(0xFF0D638B),
        decorativeIdentity = ModernDecorativeIdentity.CYAN_WATER_SKY,
        artworkIdentity = ModernArtworkIdentity.CYAN_WATER_SKY,
    )

    val Emerald = ModernThemeDefinition(
        preset = ModernThemePreset.EMERALD,
        primarySeed = Color(0xFF039B79),
        secondarySeed = Color(0xFF7BDFC5),
        tertiarySeed = Color(0xFF0D958B),
        decorativeIdentity = ModernDecorativeIdentity.EMERALD_FOLIAGE,
        artworkIdentity = ModernArtworkIdentity.EMERALD_FOLIAGE,
    )

    val Amber = ModernThemeDefinition(
        preset = ModernThemePreset.AMBER,
        primarySeed = Color(0xFFCE6107),
        secondarySeed = Color(0xFFFBA916),
        tertiarySeed = Color(0xFFFDE252),
        decorativeIdentity = ModernDecorativeIdentity.AMBER_MAPLE_SUNSET,
        artworkIdentity = ModernArtworkIdentity.AMBER_MAPLE_SUNSET,
    )

    val authoredBuiltIns: List<ModernThemeDefinition> = listOf(
        Lalapo,
        Sakura,
        Violet,
        Cyan,
        Emerald,
        Amber,
    )

    /** A dynamic definition sourced from explicit user Custom colors, not a frozen built-in. */
    fun custom(colors: ModernCustomColors): ModernThemeDefinition = ModernThemeDefinition(
        preset = ModernThemePreset.CUSTOM,
        primarySeed = colors.primary,
        secondarySeed = colors.secondary,
        tertiarySeed = colors.tertiary,
        decorativeIdentity = ModernDecorativeIdentity.CUSTOM_WAVES,
        artworkIdentity = ModernArtworkIdentity.CUSTOM_WAVES,
    )

    /** Static lookup remains for the six authored built-ins. Custom requires user colors. */
    fun find(preset: ModernThemePreset): ModernThemeDefinition? = when (preset) {
        ModernThemePreset.LALAPO -> Lalapo
        ModernThemePreset.SAKURA -> Sakura
        ModernThemePreset.VIOLET -> Violet
        ModernThemePreset.CYAN -> Cyan
        ModernThemePreset.EMERALD -> Emerald
        ModernThemePreset.AMBER -> Amber
        ModernThemePreset.CUSTOM -> null
    }
}
