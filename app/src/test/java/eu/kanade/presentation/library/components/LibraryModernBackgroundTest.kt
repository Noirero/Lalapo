package eu.kanade.presentation.library.components

import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.theme.ModernThemeDefinitions
import eu.kanade.presentation.theme.ModernThemeResolver
import eu.kanade.presentation.theme.ModernVisualPalette
import eu.kanade.presentation.theme.resolveModernVisualPalette
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LibraryModernBackgroundTest {

    @Test
    fun `Classic has no Library decoration`() {
        assertNull(modernLibraryBackgroundStops(ModernVisualPalette.Standard))
    }

    @Test
    fun `Lalapo Modern Library background uses canonical palette stops`() {
        for (dark in listOf(false, true)) {
            val scheme = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, dark, false)
            val palette = resolveModernVisualPalette(
                colorScheme = scheme,
                enabled = true,
                definition = ModernThemeDefinitions.Lalapo,
            )

            assertEquals(
                listOf(
                    palette.backgroundGradientStart,
                    palette.backgroundGradientMiddle,
                    palette.backgroundGradientEnd,
                ),
                modernLibraryBackgroundStops(palette),
            )
            assertTrue(modernLibraryBackgroundStops(palette)!!.all { it != Color.Unspecified })
        }
    }

    @Test
    fun `Modern AMOLED Library background has no tinted fullscreen stops`() {
        val scheme = ModernThemeResolver.resolve(ModernThemeDefinitions.Lalapo, true, true)
        val palette = resolveModernVisualPalette(
            colorScheme = scheme,
            enabled = true,
            definition = ModernThemeDefinitions.Lalapo,
            isAmoled = true,
        )

        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
        assertEquals(
            listOf(Color.Black, Color.Black, Color.Black),
            modernLibraryBackgroundStops(palette),
        )
    }
}
