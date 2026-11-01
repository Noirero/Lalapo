package eu.kanade.presentation.more.settings.widget

import androidx.compose.ui.graphics.Color
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.presentation.theme.ModernThemeDefinitions
import eu.kanade.presentation.theme.resolveDesignStyleColorScheme
import eu.kanade.presentation.theme.colorscheme.CatppuccinColorScheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference

class AppearanceSelectionWidgetTest {

    @Test
    fun `fresh style defaults Classic and shared AMOLED does not change selections`() {
        val style = InMemoryPreference<DesignStyle>("pref_design_style", null, DesignStyle.CLASSIC)
        val mode = InMemoryPreference<ThemeMode>("pref_theme_mode_key", null, ThemeMode.SYSTEM)
        val amoled = InMemoryPreference("pref_theme_dark_amoled_key", false, false)
        val classic = InMemoryPreference("pref_app_theme", AppTheme.CATPPUCCIN, AppTheme.DEFAULT)
        val modern = InMemoryPreference("pref_modern_theme_preset", ModernThemePreset.LALAPO, ModernThemePreset.LALAPO)

        assertEquals(DesignStyle.CLASSIC, style.get())
        assertEquals(ThemeMode.SYSTEM, mode.get())
        style.set(DesignStyle.MODERN)
        mode.set(ThemeMode.DARK)
        amoled.set(true)
        style.set(DesignStyle.CLASSIC)

        assertEquals(AppTheme.CATPPUCCIN, classic.get())
        assertEquals(ModernThemePreset.LALAPO, modern.get())
        assertTrue(amoled.get())
        assertEquals(ThemeMode.DARK, mode.get())
    }

    @Test
    fun `Classic preview remains Classic independently of global Modern selection`() {
        val preview = AppearancePreviewSelection.classic(AppTheme.CATPPUCCIN)
        assertEquals(DesignStyle.CLASSIC, preview.designStyle)
        assertEquals(AppTheme.CATPPUCCIN, preview.appTheme)
        val classicScheme = CatppuccinColorScheme.getColorScheme(
            isDark = false,
            isAmoled = false,
            overrideDarkSurfaceContainers = true,
        )
        var classicCalls = 0
        val resolved = resolveDesignStyleColorScheme(
            designStyle = preview.designStyle,
            modernThemePreset = preview.modernPreset,
            isDark = false,
            isAmoled = false,
        ) {
            classicCalls++
            classicScheme
        }
        assertSame(classicScheme, resolved)
        assertEquals(1, classicCalls)
    }

    @Test
    fun `Modern preview remains Modern independently of global Classic selection`() {
        val preview = AppearancePreviewSelection.modern(ModernThemePreset.LALAPO)
        assertEquals(DesignStyle.MODERN, preview.designStyle)
        assertEquals(ModernThemePreset.LALAPO, preview.modernPreset)
        var classicCalls = 0
        val resolved = resolveDesignStyleColorScheme(
            designStyle = preview.designStyle,
            modernThemePreset = preview.modernPreset,
            isDark = true,
            isAmoled = true,
        ) {
            classicCalls++
            CatppuccinColorScheme.darkScheme
        }
        assertEquals(0, classicCalls)
        assertEquals(Color.Black, resolved.background)
        assertNotEquals(CatppuccinColorScheme.darkScheme.primary, resolved.primary)
    }

    @Test
    fun `only real authored production presets may be shown or selected`() {
        val authored = ModernThemePreset.entries.filter { ModernThemeDefinitions.find(it) != null }
        assertEquals(listOf(ModernThemePreset.LALAPO), authored)
        ModernThemePreset.entries.filterNot { it == ModernThemePreset.LALAPO }.forEach { preset ->
            assertFalse(preset in authored)
            assertEquals(null, ModernThemeDefinitions.find(preset))
        }
    }
}
