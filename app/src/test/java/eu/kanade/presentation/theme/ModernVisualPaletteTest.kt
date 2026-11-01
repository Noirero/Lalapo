package eu.kanade.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import eu.kanade.presentation.theme.colorscheme.CatppuccinColorScheme
import eu.kanade.presentation.theme.colorscheme.CloudflareColorScheme
import eu.kanade.presentation.theme.colorscheme.CottoncandyColorScheme
import eu.kanade.presentation.theme.colorscheme.DoomColorScheme
import eu.kanade.presentation.theme.colorscheme.GreenAppleColorScheme
import eu.kanade.presentation.theme.colorscheme.LavenderColorScheme
import eu.kanade.presentation.theme.colorscheme.MatrixColorScheme
import eu.kanade.presentation.theme.colorscheme.MidnightDuskColorScheme
import eu.kanade.presentation.theme.colorscheme.MochaColorScheme
import eu.kanade.presentation.theme.colorscheme.MonetCompatColorScheme
import eu.kanade.presentation.theme.colorscheme.MonochromeColorScheme
import eu.kanade.presentation.theme.colorscheme.NordColorScheme
import eu.kanade.presentation.theme.colorscheme.SapphireColorScheme
import eu.kanade.presentation.theme.colorscheme.StrawberryColorScheme
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme
import eu.kanade.presentation.theme.colorscheme.TakoColorScheme
import eu.kanade.presentation.theme.colorscheme.TealTurqoiseColorScheme
import eu.kanade.presentation.theme.colorscheme.TidalWaveColorScheme
import eu.kanade.presentation.theme.colorscheme.TokyoNightColorScheme
import eu.kanade.presentation.theme.colorscheme.YinYangColorScheme
import eu.kanade.presentation.theme.colorscheme.YotsubaColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernVisualPaletteTest {

    @Test
    fun `standard palette is a no-op`() {
        val palette = resolveModernVisualPalette(
            colorScheme = TachiyomiColorScheme.lightScheme,
            enabled = false,
        )

        assertEquals(ModernVisualPalette.Standard, palette)
        assertEquals(Color.Unspecified, palette.backgroundGradientStart)
        assertEquals(Color.Transparent, palette.borderHighlight)
        assertEquals(Color.Transparent, palette.glow)
    }

    @Test
    fun `modern roles preserve every existing static color scheme`() {
        staticColorSchemes().forEach { scheme ->
            val snapshot = scheme.copy()
            val palette = resolveModernVisualPalette(
                colorScheme = scheme,
                enabled = true,
            )

            assertEquals(snapshot, scheme)
            assertTrue(palette.isModern)
            assertNotEquals(Color.Unspecified, palette.backgroundGradientStart)
            assertNotEquals(Color.Unspecified, palette.surfaceGradientStart)
            assertReadableGradients(scheme, palette)
        }
    }

    @Test
    fun `amoled foundation remains owned by the existing theme engine`() {
        val scheme = TachiyomiColorScheme.getColorScheme(
            isDark = true,
            isAmoled = true,
            overrideDarkSurfaceContainers = true,
        )
        val snapshot = scheme.copy()

        resolveModernVisualPalette(
            colorScheme = scheme,
            enabled = true,
        )

        assertEquals(snapshot, scheme)
        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
    }

    @Test
    fun `dynamic color schemes can derive readable modern roles`() {
        listOf(false, true).forEach { dark ->
            val scheme = MonetCompatColorScheme.generateColorSchemeFromSeed(
                seed = Color(0xFF6750A4),
                dark = dark,
            )
            val snapshot = scheme.copy()
            val palette = resolveModernVisualPalette(
                colorScheme = scheme,
                enabled = true,
            )

            assertEquals(snapshot, scheme)
            assertReadableGradients(scheme, palette)
        }
    }

    private fun staticColorSchemes(): List<ColorScheme> {
        val schemes = listOf(
            TachiyomiColorScheme,
            CatppuccinColorScheme,
            CloudflareColorScheme,
            CottoncandyColorScheme,
            DoomColorScheme,
            GreenAppleColorScheme,
            LavenderColorScheme,
            MatrixColorScheme,
            MidnightDuskColorScheme,
            MochaColorScheme,
            MonochromeColorScheme,
            NordColorScheme,
            SapphireColorScheme,
            StrawberryColorScheme,
            TakoColorScheme,
            TealTurqoiseColorScheme,
            TidalWaveColorScheme,
            TokyoNightColorScheme,
            YinYangColorScheme,
            YotsubaColorScheme,
        )

        return schemes.flatMap { listOf(it.lightScheme, it.darkScheme) }
    }

    private fun assertReadableGradients(
        scheme: ColorScheme,
        palette: ModernVisualPalette,
    ) {
        assertContrast(
            foreground = palette.accentGradientContentColor,
            backgrounds = listOf(
                palette.accentGradientStart,
                palette.accentGradientMiddle,
                palette.accentGradientEnd,
            ),
        )
        assertContrast(
            foreground = scheme.onBackground,
            backgrounds = listOf(
                palette.backgroundGradientStart,
                palette.backgroundGradientMiddle,
                palette.backgroundGradientEnd,
            ),
        )
        assertContrast(
            foreground = scheme.onSurface,
            backgrounds = listOf(
                palette.surfaceGradientStart,
                palette.surfaceGradientMiddle,
                palette.surfaceGradientEnd,
            ),
        )
    }

    private fun assertContrast(
        foreground: Color,
        backgrounds: List<Color>,
    ) {
        backgrounds.forEach { background ->
            val ratio = contrastRatio(foreground, background)
            assertTrue("contrast $ratio is below 4.5", ratio >= 4.5f)
        }
    }

    private fun contrastRatio(
        first: Color,
        second: Color,
    ): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
