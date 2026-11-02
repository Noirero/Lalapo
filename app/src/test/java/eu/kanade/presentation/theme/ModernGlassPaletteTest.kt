package eu.kanade.presentation.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModernGlassPaletteTest {

    @Test
    fun `authored built-in glass family keeps Miyorare role strengths`() {
        for (definition in ModernThemeDefinitions.authoredBuiltIns) {
            for (dark in listOf(false, true)) {
                val scheme = ModernThemeResolver.resolve(definition, dark, false)
                val visual = resolveModernVisualPalette(
                    colorScheme = scheme,
                    enabled = true,
                    definition = definition,
                )
                val glass = visual.resolveGlassPalette(scheme)

                assertEquals(84f / 255f, glass.surface.alpha, 0.001f)
                assertEquals(108f / 255f, glass.surfaceStrong.alpha, 0.001f)
                assertEquals(106f / 255f, glass.railSurface.alpha, 0.001f)
                assertEquals(208f / 255f, glass.border.alpha, 0.001f)
                assertEquals(246f / 255f, glass.borderStrong.alpha, 0.001f)
                assertEquals(176f / 255f, glass.selectedSurface.alpha, 0.001f)
                assertEquals(1f, glass.selectedBorder.alpha, 0.001f)
                assertEquals(250f / 255f, glass.innerHighlight.alpha, 0.001f)
                assertEquals(176f / 255f, glass.glow.alpha, 0.001f)
                assertEquals(228f / 255f, glass.selectedGlow.alpha, 0.001f)
                assertEquals(104f / 255f, glass.cardGlow.alpha, 0.001f)
                assertTrue(glass.content != Color.Unspecified)
            }
        }
    }
}
