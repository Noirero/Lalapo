package eu.kanade.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModernBuiltInPresetTest {

    @Test
    fun `six visual-reference-derived identities and decorative roles are frozen`() {
        val expected = listOf(
            Identity(ModernThemePreset.LALAPO, 0xFF485CE0, 0xFF04BBD4, 0xFFE84AAC,
                ModernDecorativeIdentity.LALAPO_AURORA, ModernArtworkIdentity.LALAPO_AURORA),
            Identity(ModernThemePreset.SAKURA, 0xFFC6436A, 0xFFF7A092, 0xFFFD62A2,
                ModernDecorativeIdentity.SAKURA_BLOSSOM, ModernArtworkIdentity.SAKURA_BLOSSOM),
            Identity(ModernThemePreset.VIOLET, 0xFF6F25C4, 0xFFA164E0, 0xFFD763F2,
                ModernDecorativeIdentity.VIOLET_MOON_AURORA, ModernArtworkIdentity.VIOLET_MOON_AURORA),
            Identity(ModernThemePreset.CYAN, 0xFF048DA8, 0xFF6CCEE7, 0xFF0D638B,
                ModernDecorativeIdentity.CYAN_WATER_SKY, ModernArtworkIdentity.CYAN_WATER_SKY),
            Identity(ModernThemePreset.EMERALD, 0xFF039B79, 0xFF7BDFC5, 0xFF0D958B,
                ModernDecorativeIdentity.EMERALD_FOLIAGE, ModernArtworkIdentity.EMERALD_FOLIAGE),
            Identity(ModernThemePreset.AMBER, 0xFFCE6107, 0xFFFBA916, 0xFFFDE252,
                ModernDecorativeIdentity.AMBER_MAPLE_SUNSET, ModernArtworkIdentity.AMBER_MAPLE_SUNSET),
        )
        assertEquals(expected.map { it.preset }, ModernThemeDefinitions.authoredBuiltIns.map { it.preset })
        for (entry in expected) {
            val actual = ModernThemeDefinitions.find(entry.preset)!!
            assertEquals(Color(entry.primary), actual.primarySeed, entry.preset.toString())
            assertEquals(Color(entry.secondary), actual.secondarySeed, entry.preset.toString())
            assertEquals(Color(entry.tertiary), actual.tertiarySeed, entry.preset.toString())
            assertEquals(entry.decorative, actual.decorativeIdentity)
            assertEquals(entry.artwork, actual.artworkIdentity)
        }
        assertNull(ModernThemeDefinitions.find(ModernThemePreset.CUSTOM))
    }

    @Test
    fun `all authored presets resolve deterministic readable Light Dark and AMOLED schemes`() {
        for (definition in ModernThemeDefinitions.authoredBuiltIns) {
            for (dark in listOf(false, true)) {
                for (amoled in listOf(false, true)) {
                    val scheme = ModernThemeResolver.resolve(definition, dark, amoled)
                    assertEquals(scheme, ModernThemeResolver.resolve(definition, dark, amoled))
                    listOf(
                        scheme.primary to scheme.onPrimary,
                        scheme.primaryContainer to scheme.onPrimaryContainer,
                        scheme.secondary to scheme.onSecondary,
                        scheme.secondaryContainer to scheme.onSecondaryContainer,
                        scheme.tertiary to scheme.onTertiary,
                        scheme.tertiaryContainer to scheme.onTertiaryContainer,
                        scheme.background to scheme.onBackground,
                        scheme.surface to scheme.onSurface,
                    ).forEach { (background, text) ->
                        assertReadable(text, background, definition.preset.toString())
                    }

                    val palette = resolveModernVisualPalette(
                        colorScheme = scheme,
                        enabled = true,
                        definition = definition,
                        isAmoled = dark && amoled,
                    )
                    assertTrue(palette.isModern)
                    listOf(
                        palette.accentGradientStart,
                        palette.accentGradientMiddle,
                        palette.accentGradientEnd,
                        palette.activeGradientStart,
                        palette.activeGradientEnd,
                    ).forEach { background ->
                        assertReadable(palette.accentGradientContentColor, background, definition.preset.toString())
                    }
                    listOf(
                        palette.backgroundGradientStart,
                        palette.backgroundGradientMiddle,
                        palette.backgroundGradientEnd,
                    ).forEach { background ->
                        assertReadable(scheme.onBackground, background, definition.preset.toString())
                    }

                    if (dark && amoled) {
                        assertEquals(Color.Black, scheme.background)
                        assertEquals(Color.Black, scheme.surface)
                        assertEquals(Color(0xFF0C0C0C), scheme.surfaceContainer)
                        assertEquals(Color.Black, palette.backgroundGradientStart)
                        assertEquals(Color.Black, palette.backgroundGradientMiddle)
                        assertEquals(Color.Black, palette.backgroundGradientEnd)
                        assertEquals(Color.Transparent, palette.glow)
                        assertEquals(
                            ModernThemeResolver.resolve(definition, true, false).primary,
                            scheme.primary,
                        )
                    }
                    if (!dark && amoled) assertNotEquals(Color.Black, scheme.background)
                }
            }
        }
    }

    @Test
    fun `preset colors never invoke Classic and all built-ins have individual semantic identities`() {
        val classicSentinel = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, false, false)
        for (definition in ModernThemeDefinitions.authoredBuiltIns) {
            val result = resolveDesignStyleColorScheme(
                designStyle = DesignStyle.MODERN,
                modernThemePreset = definition.preset,
                isDark = false,
                isAmoled = false,
                classicResolver = { error("Classic must not be invoked for Modern") },
            )
            assertEquals(ModernThemeResolver.resolve(definition, false, false), result)
            if (definition.preset != ModernThemePreset.LALAPO) {
                assertNotEquals(classicSentinel.primary, result.primary)
            }
        }
        assertFalse(ModernThemeDefinitions.authoredBuiltIns.any { it.preset == ModernThemePreset.CUSTOM })
    }

    private fun assertReadable(foreground: Color, background: Color, preset: String) {
        val high = maxOf(foreground.luminance(), background.luminance())
        val low = minOf(foreground.luminance(), background.luminance())
        assertTrue((high + 0.05f) / (low + 0.05f) >= 4.5f, "Contrast failed for $preset")
    }

    private data class Identity(
        val preset: ModernThemePreset,
        val primary: Long,
        val secondary: Long,
        val tertiary: Long,
        val decorative: ModernDecorativeIdentity,
        val artwork: ModernArtworkIdentity,
    )
}
