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
}

internal enum class ModernArtworkIdentity {
    LALAPO_AURORA,
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

    /** Future identities have no production definition until their authored stage. */
    fun find(preset: ModernThemePreset): ModernThemeDefinition? = when (preset) {
        ModernThemePreset.LALAPO -> Lalapo
        else -> null
    }
}
