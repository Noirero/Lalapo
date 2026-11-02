package eu.kanade.presentation.anime.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import eu.kanade.presentation.theme.CinematicAccentPalette
import eu.kanade.presentation.theme.ModernCustomColors
import eu.kanade.presentation.theme.ModernThemeDefinitions
import eu.kanade.presentation.theme.ModernThemeResolver
import eu.kanade.presentation.theme.ModernVisualPalette
import eu.kanade.presentation.theme.resolveAccentPalette
import eu.kanade.presentation.theme.resolveModernVisualPalette
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimeModernThemeIntegrationTest {

    private val coolArtwork = AnimeArtworkPaletteSeeds(
        primary = Color(0xFF1762BF),
        secondary = Color(0xFF14A3CE),
        tertiary = Color(0xFF7048C4),
    )

    private val warmArtwork = AnimeArtworkPaletteSeeds(
        primary = Color(0xFFBF4318),
        secondary = Color(0xFFE17B25),
        tertiary = Color(0xFFA62B64),
    )

    @Test
    fun `Lalapo Modern Light Dark and AMOLED keep artwork accents local and readable`() {
        for ((dark, amoled) in listOf(false to false, true to false, true to true)) {
            val material = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, dark, amoled)
            val snapshot = material.snapshot()
            val base = resolveModernVisualPalette(
                colorScheme = material,
                enabled = true,
                definition = ModernThemeDefinitions.Lalapo,
                isAmoled = dark && amoled,
            )
            val globalFallback = base.resolveAccentPalette(null)
            for (seeds in listOf(warmArtwork, coolArtwork)) {
                val local = deriveCinematicAccentPalette(seeds, material)
                assertEquals(local, base.resolveAccentPalette(local))
                assertContrast(local.accentGradientContentColor, local.accentGradientStart)
                assertContrast(local.accentGradientContentColor, local.accentGradientMiddle)
                assertContrast(local.accentGradientContentColor, local.accentGradientEnd)
                assertContrast(local.accentGradientContentColor, local.activeGradientStart)
                assertContrast(local.accentGradientContentColor, local.activeGradientEnd)
                assertEquals(snapshot, material.snapshot(), "Scoped artwork accent mutated global Material")
                assertEquals(
                    globalFallback,
                    base.resolveAccentPalette(null),
                    "Scoped artwork accent leaked into an unscoped palette",
                )
                assertEquals(base.accentGradientStart, globalFallback.accentGradientStart)
                assertEquals(base.accentGradientEnd, globalFallback.accentGradientEnd)
            }
        }
    }

    @Test
    fun `missing invalid and low-chroma artwork fallback to Lalapo semantic accents`() {
        val material = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, true, true)
        val base = resolveModernVisualPalette(
            colorScheme = material,
            enabled = true,
            definition = ModernThemeDefinitions.Lalapo,
            isAmoled = true,
        )
        // Failed extraction (missing or invalid image) and rejected monochrome swatches
        // both produce null seeds, which must preserve the global Modern palette.
        val missingOrRejectedArtwork: AnimeArtworkPaletteSeeds? = null
        val accent = missingOrRejectedArtwork?.let { deriveCinematicAccentPalette(it, material) }
        val fallback = base.resolveAccentPalette(accent)

        assertEquals(base.accentGradientStart, fallback.accentGradientStart)
        assertEquals(base.accentGradientMiddle, fallback.accentGradientMiddle)
        assertEquals(base.accentGradientEnd, fallback.accentGradientEnd)
        assertEquals(base.activeGradientStart, fallback.activeGradientStart)
        assertEquals(base.activeGradientEnd, fallback.activeGradientEnd)
        assertEquals(base.accentGradientContentColor, fallback.accentGradientContentColor)
        assertEquals(Color.Black, material.background)
        assertContrast(fallback.accentGradientContentColor, fallback.accentGradientStart)
        assertContrast(fallback.accentGradientContentColor, fallback.accentGradientEnd)

        val local = deriveCinematicAccentPalette(coolArtwork, material)
        assertNotEquals(local, fallback)
        assertEquals(
            CinematicAccentPalette.Standard,
            ModernVisualPalette.Standard.resolveAccentPalette(local),
            "Classic appearance must never show Modern cinematic artwork",
        )
    }

    @Test
    fun `warm Amber and cool Cyan preserve local artwork and fallback across Light Dark AMOLED`() {
        for (definition in listOf(ModernThemeDefinitions.Amber, ModernThemeDefinitions.Cyan)) {
            for ((dark, amoled) in listOf(false to false, true to false, true to true)) {
                val scheme = ModernThemeResolver.resolve(definition, dark, amoled)
                val snapshot = scheme.snapshot()
                val palette = resolveModernVisualPalette(
                    colorScheme = scheme,
                    enabled = true,
                    definition = definition,
                    isAmoled = dark && amoled,
                )
                val fallback = palette.resolveAccentPalette(null)

                for (seeds in listOf(warmArtwork, coolArtwork)) {
                    val cinematic = deriveCinematicAccentPalette(seeds, scheme)
                    assertContrast(cinematic.accentGradientContentColor, cinematic.accentGradientStart)
                    assertContrast(cinematic.accentGradientContentColor, cinematic.activeGradientEnd)
                    assertEquals(cinematic, palette.resolveAccentPalette(cinematic))
                    assertEquals(fallback, palette.resolveAccentPalette(null))
                    assertEquals(snapshot, scheme.snapshot())
                }

                // Invalid / missing and monochrome artwork extraction yields null seeds.
                val unavailableSeeds: AnimeArtworkPaletteSeeds? = null
                assertEquals(
                    fallback,
                    palette.resolveAccentPalette(unavailableSeeds?.let { deriveCinematicAccentPalette(it, scheme) }),
                )
            }
        }
    }

    @Test
    fun `warm cool neutral Custom themes preserve local artwork and invalid fallback`() {
        for (input in listOf("#F57015", "#00A9EA", "#7B7B7B")) {
            val definition = ModernThemeDefinitions.custom(ModernCustomColors.fromStored(input, "", ""))
            for ((dark, amoled) in listOf(false to false, true to false, true to true)) {
                val scheme = ModernThemeResolver.resolve(definition, dark, amoled)
                val initial = scheme.snapshot()
                val visual = resolveModernVisualPalette(
                    colorScheme = scheme,
                    enabled = true,
                    definition = definition,
                    isAmoled = dark && amoled,
                )
                val noArtwork = visual.resolveAccentPalette(null)
                for (artwork in listOf(warmArtwork, coolArtwork)) {
                    val local = deriveCinematicAccentPalette(artwork, scheme)
                    assertEquals(local, visual.resolveAccentPalette(local))
                    assertContrast(local.accentGradientContentColor, local.accentGradientStart)
                    assertContrast(local.accentGradientContentColor, local.accentGradientEnd)
                    assertEquals(initial, scheme.snapshot())
                    assertEquals(noArtwork, visual.resolveAccentPalette(null))
                }
                // Failed/monochrome extraction returns no seeds and cannot mutate Custom.
                val invalidArtwork: AnimeArtworkPaletteSeeds? = null
                assertEquals(
                    noArtwork,
                    visual.resolveAccentPalette(invalidArtwork?.let { deriveCinematicAccentPalette(it, scheme) }),
                )
            }
        }
    }

    private fun ColorScheme.snapshot(): List<Color> = listOf(
        primary,
        onPrimary,
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
        outline,
    )

    private fun assertContrast(foreground: Color, background: Color) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val ratio = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue(ratio >= 4.5f, "Contrast $ratio below 4.5")
    }
}
