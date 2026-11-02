package eu.kanade.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    fun `physical QA correction keeps square-ish cards and isolated title contrast`() {
        val modern = ModernVisualPalette.Standard.copy(isModern = true)
        val entries = ModernThemeDefinitions.authoredBuiltIns.map {
            requireNotNull(resolveModernLibraryChrome(modern, it.artworkIdentity))
        }
        entries.forEach { chrome ->
            org.junit.jupiter.api.Assertions.assertTrue(chrome.cardAspectRatio in 0.9f..1.0f)
        }
        assertNotEquals(entries[1].cardShape, entries[2].cardShape)
        assertEquals(Color(0xFF9C2257), entries[1].titleColor)
        assertEquals(Color(0xFF51152E), entries[1].headerSubtitleColor)
        assertNotEquals(entries[1].navSelectedShape, entries[2].navSelectedShape)
        assertNotEquals(entries[1].segmentedShape, entries[2].segmentedShape)
        assertEquals(0.34f, entries[1].setSelectorWeight)
        assertEquals(0.38f, entries[0].setSelectorWeight)
        entries.forEach { chrome ->
            org.junit.jupiter.api.Assertions.assertTrue(chrome.setSelectorWeight in 0.3f..0.5f)
        }
        assertNull(resolveModernLibraryChrome(modern, ModernArtworkIdentity.CUSTOM_WAVES))
    }

    @Test
    fun `set selector protects Utama label without losing category scrolling space`() {
        assertEquals(184.dp, modernLibrarySetSelectorWidth(400.dp, 0.34f))
        assertEquals(560.dp * 0.34f, modernLibrarySetSelectorWidth(560.dp, 0.34f))
        assertEquals(280.dp * 0.52f, modernLibrarySetSelectorWidth(280.dp, 0.34f))
        val modern = ModernVisualPalette.Standard.copy(isModern = true)
        val violet = requireNotNull(
            resolveModernLibraryChrome(modern, ModernArtworkIdentity.VIOLET_MOON_AURORA),
        )
        assertEquals(RoundedCornerShape(28.dp), violet.navSelectedShape)
        assertEquals(0.95f, violet.cardAspectRatio)
        assertNull(resolveModernLibraryChrome(modern, ModernArtworkIdentity.CUSTOM_WAVES))
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
