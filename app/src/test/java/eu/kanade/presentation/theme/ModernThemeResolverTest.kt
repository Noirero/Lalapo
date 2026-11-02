package eu.kanade.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import eu.kanade.presentation.theme.colorscheme.CatppuccinColorScheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModernThemeResolverTest {

    @Test
    fun `Lalapo identity is stable and Custom remains unauthored`() {
        val definition = ModernThemeDefinitions.Lalapo

        assertEquals(ModernThemePreset.LALAPO, definition.preset)
        assertEquals(Color(0xFF485CE0), definition.primarySeed)
        assertEquals(Color(0xFF04BBD4), definition.secondarySeed)
        assertEquals(Color(0xFFE84AAC), definition.tertiarySeed)
        assertEquals(ModernDecorativeIdentity.LALAPO_AURORA, definition.decorativeIdentity)
        assertEquals(ModernArtworkIdentity.LALAPO_AURORA, definition.artworkIdentity)
        assertEquals(definition, ModernThemeDefinitions.find(ModernThemePreset.LALAPO))
        assertEquals(null, ModernThemeDefinitions.find(ModernThemePreset.CUSTOM))
    }

    @Test
    fun `Lalapo resolves deterministic readable Light and Dark Material schemes`() {
        for (dark in listOf(false, true)) {
            val scheme = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, dark, false)
            assertEquals(scheme, ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, dark, false))
            assertMaterialContrast(scheme)
            val palette = resolveModernVisualPalette(
                colorScheme = scheme,
                enabled = true,
                definition = ModernThemeDefinitions.Lalapo,
            )
            assertTrue(palette.isModern)
            assertContrast(palette.accentGradientContentColor, palette.accentGradientStart)
            assertContrast(palette.accentGradientContentColor, palette.accentGradientMiddle)
            assertContrast(palette.accentGradientContentColor, palette.accentGradientEnd)
            assertContrast(palette.accentGradientContentColor, palette.activeGradientStart)
            assertContrast(palette.accentGradientContentColor, palette.activeGradientEnd)
            assertContrast(scheme.onBackground, palette.backgroundGradientStart)
            assertContrast(scheme.onBackground, palette.backgroundGradientMiddle)
            assertContrast(scheme.onBackground, palette.backgroundGradientEnd)
        }
        val light = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, false, false)
        val dark = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, true, false)
        assertNotEquals(light.primary, dark.primary)
        assertNotEquals(light.background, dark.background)
    }

    @Test
    fun `Modern AMOLED retains BaseColorScheme black body and neutral containers`() {
        val normalDark = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, true, false)
        val amoled = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, true, true)
        val light = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, false, true)
        assertEquals(Color.Black, amoled.background)
        assertEquals(Color.Black, amoled.surface)
        assertEquals(Color.White, amoled.onBackground)
        assertEquals(Color(0xFF0C0C0C), amoled.surfaceContainer)
        assertEquals(normalDark.primary, amoled.primary)
        assertEquals(normalDark.onPrimary, amoled.onPrimary)
        assertNotEquals(Color.Black, light.background)

        val palette = resolveModernVisualPalette(
            colorScheme = amoled,
            enabled = true,
            definition = ModernThemeDefinitions.Lalapo,
            isAmoled = true,
        )
        assertEquals(Color.Black, palette.backgroundGradientStart)
        assertEquals(Color.Black, palette.backgroundGradientMiddle)
        assertEquals(Color.Black, palette.backgroundGradientEnd)
        assertEquals(Color.Transparent, palette.glow)
    }

    @Test
    fun `Classic remains on the exact existing resolver and ignores Modern preset`() {
        val classic = CatppuccinColorScheme.getColorScheme(
            isDark = false,
            isAmoled = false,
            overrideDarkSurfaceContainers = true,
        )
        var invocations = 0
        val result = resolveDesignStyleColorScheme(
            designStyle = DesignStyle.CLASSIC,
            modernThemePreset = ModernThemePreset.VIOLET,
            isDark = false,
            isAmoled = false,
            classicResolver = {
                invocations++
                classic
            },
        )
        assertSame(classic, result)
        assertEquals(1, invocations)
        assertEquals(CatppuccinColorScheme.lightScheme, result)
        assertEquals(ModernVisualPalette.Standard, resolveModernVisualPalette(result, enabled = false))

        val modern = resolveDesignStyleColorScheme(
            designStyle = DesignStyle.MODERN,
            modernThemePreset = ModernThemePreset.LALAPO,
            isDark = false,
            isAmoled = false,
            classicResolver = {
                invocations++
                classic
            },
        )
        assertEquals(1, invocations)
        assertNotEquals(classic.primary, modern.primary)
    }

    @Test
    fun `unimplemented Modern identities are rejected instead of masquerading as Lalapo`() {
        listOf(ModernThemePreset.CUSTOM).forEach { preset ->
            assertThrows(IllegalArgumentException::class.java) {
                resolveDesignStyleColorScheme(
                    designStyle = DesignStyle.MODERN,
                    modernThemePreset = preset,
                    isDark = false,
                    isAmoled = false,
                    classicResolver = { error("Classic resolver must not be called") },
                )
            }
            assertFalse(ModernThemeDefinitions.find(preset) != null)
        }
    }

    private fun assertMaterialContrast(scheme: ColorScheme) {
        listOf(
            scheme.primary to scheme.onPrimary,
            scheme.primaryContainer to scheme.onPrimaryContainer,
            scheme.secondary to scheme.onSecondary,
            scheme.secondaryContainer to scheme.onSecondaryContainer,
            scheme.tertiary to scheme.onTertiary,
            scheme.tertiaryContainer to scheme.onTertiaryContainer,
            scheme.background to scheme.onBackground,
            scheme.surface to scheme.onSurface,
        ).forEach { (background, foreground) -> assertContrast(foreground, background) }
    }

    private fun assertContrast(foreground: Color, background: Color) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val ratio = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue(ratio >= 4.5f, "contrast $ratio below 4.5")
    }
}
