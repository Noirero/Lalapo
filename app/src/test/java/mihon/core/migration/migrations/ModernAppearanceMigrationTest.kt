package mihon.core.migration.migrations

import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference

class ModernAppearanceMigrationTest {

    @Test
    fun `legacy false becomes Classic without selecting a Modern preset`() {
        val legacy = InMemoryPreference("pref_modern_appearance", false, false)
        val style = stylePreference()
        val preset = presetPreference()

        migrateLegacyModernAppearance(legacy, style, preset)

        assertEquals(DesignStyle.CLASSIC, style.get())
        assertFalse(legacy.isSet())
        assertFalse(preset.isSet())
    }

    @Test
    fun `legacy true becomes Modern Lalapo and preserves Classic theme`() {
        val legacy = InMemoryPreference("pref_modern_appearance", true, false)
        val style = stylePreference()
        val preset = presetPreference()
        val classic = InMemoryPreference("pref_app_theme", AppTheme.CATPPUCCIN, AppTheme.DEFAULT)

        migrateLegacyModernAppearance(legacy, style, preset)

        assertEquals(DesignStyle.MODERN, style.get())
        assertEquals(ModernThemePreset.LALAPO, preset.get())
        assertTrue(preset.isSet())
        assertEquals(AppTheme.CATPPUCCIN, classic.get())
        assertFalse(legacy.isSet())
    }

    @Test
    fun `absent legacy key retains fresh-install defaults`() {
        val legacy = InMemoryPreference<Boolean>("pref_modern_appearance", null, false)
        val style = stylePreference()
        val preset = presetPreference()

        migrateLegacyModernAppearance(legacy, style, preset)

        assertEquals(DesignStyle.CLASSIC, style.get())
        assertEquals(ModernThemePreset.LALAPO, preset.get())
        assertFalse(style.isSet())
        assertFalse(preset.isSet())
    }

    @Test
    fun `migration is idempotent and never overrides explicit new selections`() {
        val legacy = InMemoryPreference("pref_modern_appearance", true, false)
        val style = stylePreference()
        val preset = presetPreference()

        migrateLegacyModernAppearance(legacy, style, preset)
        style.set(DesignStyle.CLASSIC)
        preset.set(ModernThemePreset.VIOLET)
        migrateLegacyModernAppearance(legacy, style, preset)

        assertEquals(DesignStyle.CLASSIC, style.get())
        assertEquals(ModernThemePreset.VIOLET, preset.get())

        val staleLegacy = InMemoryPreference("pref_modern_appearance", true, false)
        migrateLegacyModernAppearance(staleLegacy, style, preset)
        assertEquals(DesignStyle.CLASSIC, style.get())
        assertEquals(ModernThemePreset.VIOLET, preset.get())
    }

    @Test
    fun `Classic and Modern selections are orthogonal`() {
        val classic = InMemoryPreference("pref_app_theme", AppTheme.CATPPUCCIN, AppTheme.DEFAULT)
        val style = stylePreference()
        val preset = presetPreference()
        classic.set(AppTheme.CATPPUCCIN)
        preset.set(ModernThemePreset.LALAPO)

        style.set(DesignStyle.MODERN)
        style.set(DesignStyle.CLASSIC)

        assertEquals(AppTheme.CATPPUCCIN, classic.get())
        assertEquals(ModernThemePreset.LALAPO, preset.get())
    }

    private fun stylePreference() =
        InMemoryPreference<DesignStyle>("pref_design_style", null, DesignStyle.CLASSIC)

    private fun presetPreference() =
        InMemoryPreference<ModernThemePreset>("pref_modern_theme_preset", null, ModernThemePreset.LALAPO)
}
