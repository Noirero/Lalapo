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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModernCustomColorsTest {

    @Test
    fun `HEX normalization rejects malformed nonopaque inputs and accepts RGB only`() {
        assertEquals("#AABBCC", normalizeCustomHex(" aabbcc "))
        assertEquals("#5A00EF", normalizeCustomHex("#5a00ef"))
        assertEquals(Color(0xFFAABBCC), customHexColor("aabbcc"))
        for (invalid in listOf("", "blue", "#FFF", "#11223344", "#00112Z", "12345", "#1234567")) {
            assertNull(normalizeCustomHex(invalid), invalid)
            assertNull(customHexColor(invalid), invalid)
        }
    }

    @Test
    fun `AUTO seeds are deterministic and distinct for neutral saturated dark and light Primaries`() {
        for (input in listOf("#808080", "#FF0044", "#000020", "#FDFDFD", "#000000", "#FFFFFF", "#485CE0")) {
            val colors = ModernCustomColors.fromStored(input, "", "")
            assertEquals(colors.secondary, ModernCustomColors.fromStored(input, "", "").secondary)
            assertEquals(colors.tertiary, ModernCustomColors.fromStored(input, "", "").tertiary)
            val hues = listOf(colors.primary, colors.secondary, colors.tertiary)
            assertEquals(3, hues.distinct().size, input)
            assertNotEquals(colors.secondary, colors.tertiary, input)
            assertEquals(Color(0xFF000020), ModernCustomColors.fromStored("#000020", "", "").primary.takeIf {
                input == "#000020"
            } ?: Color(0xFF000020))
            assertEquals(customColorHex(customHexColor(customColorHex(colors.primary))!!), customColorHex(colors.primary))
        }
    }

    @Test
    fun `Secondary and Tertiary overrides take precedence and can return to AUTO`() {
        val auto = ModernCustomColors.fromStored("#8A3BFA", "", "")
        val secondary = ModernCustomColors.fromStored("#8A3BFA", "00aa88", "")
        assertEquals(Color(0xFF00AA88), secondary.secondary)
        assertEquals(auto.tertiary, secondary.tertiary)
        val both = ModernCustomColors.fromStored("#8A3BFA", "#00AA88", "#BB1122")
        assertEquals(Color(0xFFBB1122), both.tertiary)
        assertEquals(auto.secondary, ModernCustomColors.fromStored("#8A3BFA", "", "#BB1122").secondary)
        assertEquals(auto.secondary, ModernCustomColors.fromStored("#8A3BFA", "", "").secondary)
        assertEquals(auto.tertiary, ModernCustomColors.fromStored("#8A3BFA", "", "").tertiary)
        assertEquals(DEFAULT_CUSTOM_PRIMARY, customColorHex(ModernCustomColors.fromStored("garbage", "", "").primary))
    }

    @Test
    fun `Custom shares production resolver contrast and black AMOLED policy`() {
        for (primary in listOf("#808080", "#FF0044", "#000020", "#FDFDFD", "#000000", "#FFFFFF")) {
            val colors = ModernCustomColors.fromStored(primary, "", "")
            val definition = ModernThemeDefinitions.custom(colors)
            assertEquals(ModernThemePreset.CUSTOM, definition.preset)
            assertEquals(ModernArtworkIdentity.CUSTOM_WAVES, definition.artworkIdentity)
            assertEquals(ModernDecorativeIdentity.CUSTOM_WAVES, definition.decorativeIdentity)
            assertEquals(colors.secondary, definition.secondarySeed)
            for ((dark, amoled) in listOf(false to false, true to false, true to true)) {
                val actual = ModernThemeResolver.resolve(definition, dark, amoled)
                val duplicate = ModernThemeResolver.resolve(ModernThemeDefinitions.custom(colors), dark, amoled)
                assertEquals(actual.semanticSnapshot(), duplicate.semanticSnapshot())
                assertMaterialContrast(actual)
                val palette = resolveModernVisualPalette(
                    colorScheme = actual,
                    enabled = true,
                    definition = definition,
                    isAmoled = dark && amoled,
                )
                listOf(
                    palette.accentGradientStart,
                    palette.accentGradientMiddle,
                    palette.accentGradientEnd,
                    palette.activeGradientStart,
                    palette.activeGradientEnd,
                ).forEach { assertReadable(palette.accentGradientContentColor, it) }
                if (dark && amoled) {
                    assertEquals(Color.Black, actual.background)
                    assertEquals(Color.Black, actual.surface)
                    assertEquals(Color.Black, palette.backgroundGradientStart)
                    assertEquals(Color.Black, palette.backgroundGradientMiddle)
                    assertEquals(Color.Black, palette.backgroundGradientEnd)
                    assertEquals(Color.Transparent, palette.glow)
                }
            }
        }
    }

    @Test
    fun `Classic ignores Custom and existing built-in identities remain unchanged`() {
        val custom = ModernCustomColors.fromStored("#FE9000", "", "")
        val classic = CatppuccinColorScheme.lightScheme
        val result = resolveDesignStyleColorScheme(
            designStyle = DesignStyle.CLASSIC,
            modernThemePreset = ModernThemePreset.CUSTOM,
            customColors = custom,
            isDark = false,
            isAmoled = false,
            classicResolver = { classic },
        )
        assertSame(classic, result)
        assertFalse(ModernThemeDefinitions.authoredBuiltIns.any { it.preset == ModernThemePreset.CUSTOM })
        assertEquals(Color(0xFF485CE0), ModernThemeDefinitions.Lalapo.primarySeed)
        assertEquals(Color(0xFFC6436A), ModernThemeDefinitions.Sakura.primarySeed)
        assertEquals(Color(0xFF6F25C4), ModernThemeDefinitions.Violet.primarySeed)
        assertEquals(Color(0xFF048DA8), ModernThemeDefinitions.Cyan.primarySeed)
        assertEquals(Color(0xFF039B79), ModernThemeDefinitions.Emerald.primarySeed)
        assertEquals(Color(0xFFCE6107), ModernThemeDefinitions.Amber.primarySeed)

        val modern = resolveDesignStyleColorScheme(
            designStyle = DesignStyle.MODERN,
            modernThemePreset = ModernThemePreset.CUSTOM,
            customColors = custom,
            isDark = false,
            isAmoled = false,
            classicResolver = { error("Classic resolver must not be called") },
        )
        assertNotEquals(classic.primary, modern.primary)
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
        ).forEach { (background, text) -> assertReadable(text, background) }
    }

    private fun assertReadable(foreground: Color, background: Color) {
        val ratio = (maxOf(foreground.luminance(), background.luminance()) + 0.05f) /
            (minOf(foreground.luminance(), background.luminance()) + 0.05f)
        assertTrue(ratio >= 4.5f, "Contrast $ratio below 4.5")
    }

    private fun ColorScheme.semanticSnapshot(): List<Color> = listOf(
        primary,
        onPrimary,
        primaryContainer,
        onPrimaryContainer,
        secondary,
        onSecondary,
        tertiary,
        onTertiary,
        background,
        onBackground,
        surface,
        onSurface,
        surfaceContainer,
        surfaceContainerHigh,
    )
}
