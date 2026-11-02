package eu.kanade.presentation.theme

import androidx.compose.ui.graphics.Color
import eu.kanade.domain.ui.model.ModernThemePreset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModernWallpaperTest {

    @Test
    fun `six built-in identities resolve distinct authored full portraits`() {
        val resources = ModernThemeDefinitions.authoredBuiltIns.map { definition ->
            modernWallpaperResource(isModern = true, identity = definition.artworkIdentity)
        }
        assertEquals(6, resources.filterNotNull().distinct().size)
        resources.forEach { assertNotNull(it) }
        assertNull(modernWallpaperResource(isModern = false, identity = ModernArtworkIdentity.SAKURA_BLOSSOM))
        assertNull(modernWallpaperResource(isModern = true, identity = ModernArtworkIdentity.CUSTOM_WAVES))
        assertNull(modernWallpaperResource(isModern = true, identity = null))
    }

    @Test
    fun `wallpaper presentation is translucent without changing canonical resolver semantics`() {
        for (definition in ModernThemeDefinitions.authoredBuiltIns) {
            for (dark in listOf(false, true)) {
                val canonical = ModernThemeResolver.resolve(definition, dark, false)
                assertEquals(1f, canonical.background.alpha)
                val presented = resolveModernPresentationColorScheme(
                    colorScheme = canonical,
                    wallpaperEnabled = true,
                    isDark = dark,
                    isAmoled = false,
                )
                assertTrue(presented.background.alpha < 1f, definition.preset.toString())
                assertTrue(presented.surface.alpha < 1f, definition.preset.toString())
            }

            val canonicalAmoled = ModernThemeResolver.resolve(definition, true, true)
            assertEquals(Color.Black, canonicalAmoled.background)
            val presentedAmoled = resolveModernPresentationColorScheme(
                colorScheme = canonicalAmoled,
                wallpaperEnabled = true,
                isDark = true,
                isAmoled = true,
            )
            assertEquals(Color.Black, presentedAmoled.background.copy(alpha = 1f))
            assertTrue(presentedAmoled.background.alpha < 1f)
        }

        val custom = ModernThemeDefinitions.custom(ModernCustomColors.fromStored("#485CE0", "", ""))
        val customScheme = ModernThemeResolver.resolve(custom, false, false)
        assertEquals(ModernThemePreset.CUSTOM, custom.preset)
        assertEquals(
            customScheme,
            resolveModernPresentationColorScheme(
                colorScheme = customScheme,
                wallpaperEnabled = false,
                isDark = false,
                isAmoled = false,
            ),
        )
    }
}
