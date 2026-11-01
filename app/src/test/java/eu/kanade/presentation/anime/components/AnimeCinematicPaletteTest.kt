package eu.kanade.presentation.anime.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimeCinematicPaletteTest {

    @Test
    fun `artwork cache key changes when artwork identity changes`() {
        val base = animeArtworkPaletteKey(
            animeId = 42L,
            backgroundUrl = "https://example.test/background.jpg",
            backgroundLastModified = 10L,
            thumbnailUrl = "https://example.test/cover.jpg",
            coverLastModified = 20L,
        )

        assertTrue(
            base != animeArtworkPaletteKey(
                animeId = 42L,
                backgroundUrl = "https://example.test/background-2.jpg",
                backgroundLastModified = 10L,
                thumbnailUrl = "https://example.test/cover.jpg",
                coverLastModified = 20L,
            ),
        )
        assertTrue(
            base != animeArtworkPaletteKey(
                animeId = 42L,
                backgroundUrl = "https://example.test/background.jpg",
                backgroundLastModified = 11L,
                thumbnailUrl = "https://example.test/cover.jpg",
                coverLastModified = 20L,
            ),
        )
        assertTrue(
            base != animeArtworkPaletteKey(
                animeId = 42L,
                backgroundUrl = "https://example.test/background.jpg",
                backgroundLastModified = 10L,
                thumbnailUrl = "https://example.test/cover.jpg",
                coverLastModified = 21L,
            ),
        )
    }

    @Test
    fun `artwork accents remain readable across light and dark themes`() {
        val seedSets = listOf(
            AnimeArtworkPaletteSeeds(
                primary = Color(0xFF1565C0),
                secondary = Color(0xFF5E35B1),
                tertiary = Color(0xFF00838F),
            ),
            AnimeArtworkPaletteSeeds(
                primary = Color(0xFFC62828),
                secondary = Color(0xFFAD1457),
                tertiary = Color(0xFFEF6C00),
            ),
            AnimeArtworkPaletteSeeds(
                primary = Color(0xFF1C1B1F),
                secondary = Color(0xFF424047),
                tertiary = Color(0xFF625F68),
            ),
            AnimeArtworkPaletteSeeds(
                primary = Color(0xFFF4EAF7),
                secondary = Color(0xFFE6D7F3),
                tertiary = Color(0xFFFFE0E8),
            ),
        )

        listOf(
            TachiyomiColorScheme.lightScheme,
            TachiyomiColorScheme.darkScheme,
        ).forEach { scheme ->
            seedSets.forEach { seeds ->
                val snapshot = scheme.authoritySnapshot()
                val palette = deriveCinematicAccentPalette(seeds, scheme)

                assertEquals(snapshot, scheme.authoritySnapshot())
                listOf(
                    palette.accentGradientStart,
                    palette.accentGradientMiddle,
                    palette.accentGradientEnd,
                    palette.activeGradientStart,
                    palette.activeGradientEnd,
                ).forEach { background ->
                    val contrast = cinematicContrastRatio(
                        foreground = palette.accentGradientContentColor,
                        background = background,
                    )
                    assertTrue(contrast >= 4.5f, "contrast $contrast is below 4.5")
                }
            }
        }
    }

    private fun ColorScheme.authoritySnapshot(): List<Color> = listOf(
        primary,
        onPrimary,
        primaryContainer,
        onPrimaryContainer,
        secondary,
        onSecondary,
        secondaryContainer,
        onSecondaryContainer,
        tertiary,
        onTertiary,
        tertiaryContainer,
        onTertiaryContainer,
        background,
        onBackground,
        surface,
        onSurface,
        surfaceContainerLowest,
        surfaceContainerLow,
        surfaceContainer,
        surfaceContainerHigh,
        surfaceContainerHighest,
        outline,
        outlineVariant,
    )
}
