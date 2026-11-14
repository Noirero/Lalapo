package eu.kanade.presentation.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModernPilotGeometryTest {
    @Test
    fun `visual controls fit in separately reserved accessible touch bounds`() {
        val touch = ModernLibraryGeometry.TouchTarget
        assertTrue(touch.value >= 48f)
        assertTrue(ModernLibraryGeometry.SearchHeight < touch)
        assertTrue(ModernLibraryGeometry.FilterHeight < touch)
        assertTrue(ModernLibraryGeometry.CategoryVisualHeight < ModernLibraryGeometry.CategoryRailHeight)
        assertTrue(ModernPhoneNavigationGeometry.SelectedHeight >= touch)
        assertTrue(ModernPhoneNavigationGeometry.SelectedHeight <= ModernPhoneNavigationGeometry.MinHeight)
    }

    @Test
    fun `overlay typography follows measured cover bounds and remains bounded at tiny and huge sizes`() {
        assertEquals(10.5f, ModernLibraryGeometry.overlayTitleSize(128f, 152f))
        assertEquals(9f, ModernLibraryGeometry.overlayTitleSize(0f, 0f))
        assertEquals(14.7f, ModernLibraryGeometry.overlayTitleSize(1000f, 1000f), 0.001f)
        var previous = 0f
        for (width in 1..512) {
            val title = ModernLibraryGeometry.overlayTitleSize(
                width.toFloat(),
                width / ModernLibraryGeometry.COVER_ASPECT_RATIO,
            )
            assertTrue(title in 9f..15f)
            assertTrue(title >= previous)
            previous = title
        }
        val below = ModernLibraryGeometry.overlayTitleSize(128f * 0.79999f, 152f)
        val above = ModernLibraryGeometry.overlayTitleSize(128f * 0.80001f, 152f)
        assertEquals(below, above, 0.001f)
    }
}
