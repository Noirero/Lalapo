package eu.kanade.presentation.theme

import eu.kanade.domain.ui.model.ModernThemePreset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ModernHeaderArtworkTest {

    @Test
    fun `each authored built-in has one distinct centralized drawable mapping`() {
        val defs = ModernThemeDefinitions.authoredBuiltIns
        assertEquals(6, defs.size)
        val resources = defs.map { definition ->
            assertNotNull(ModernThemeDefinitions.find(definition.preset))
            modernHeaderArtworkResource(true, definition.artworkIdentity)
        }
        assertEquals(6, resources.filterNotNull().distinct().size)
        assertNull(ModernThemeDefinitions.find(ModernThemePreset.CUSTOM))
    }

    @Test
    fun `Classic never resolves a Modern hero and Modern requires an authored identity`() {
        for (definition in ModernThemeDefinitions.authoredBuiltIns) {
            assertNull(modernHeaderArtworkResource(false, definition.artworkIdentity))
        }
        assertNull(modernHeaderArtworkResource(true, null))
        assertNull(modernHeaderArtworkResource(false, null))
    }
}
