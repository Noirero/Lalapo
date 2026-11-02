package eu.kanade.presentation.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ModernLibraryChromeTest {
    @Test
    fun `only six built-in identities receive authored library chrome`() {
        assertNull(resolveModernLibraryChrome(ModernVisualPalette.Standard, ModernArtworkIdentity.LALAPO_AURORA))
        val modern = ModernVisualPalette.Standard.copy(isModern = true)
        assertNull(resolveModernLibraryChrome(modern, null))
        assertNull(resolveModernLibraryChrome(modern, ModernArtworkIdentity.CUSTOM_WAVES))
        for (definition in ModernThemeDefinitions.authoredBuiltIns) {
            val chrome = resolveModernLibraryChrome(modern, definition.artworkIdentity)
            assertNotNull(chrome, definition.preset.toString())
            assertNotEquals(Color.Transparent, chrome!!.border)
            assertNotEquals(chrome.surfaceStart, chrome.selectedStart)
            assertNotEquals(Color.Transparent, chrome.selectedBorder)
        }
    }

    @Test
    fun `reference identity ornaments and distinct selections are retained`() {
        val modern = ModernVisualPalette.Standard.copy(isModern = true)
        val expected = listOf("//", "✿", "///", "〰", "❧", "//")
        val chromes = ModernThemeDefinitions.authoredBuiltIns.map {
            requireNotNull(resolveModernLibraryChrome(modern, it.artworkIdentity))
        }
        assertEquals(expected, chromes.map { it.ornament })
        assertEquals(6, chromes.map { it.selectedStart }.distinct().size)
        assertNotEquals(chromes[1].actionShape, chromes[3].actionShape)
    }
}
