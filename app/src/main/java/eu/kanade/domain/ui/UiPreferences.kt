package eu.kanade.domain.ui

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import eu.kanade.domain.ui.model.StartScreen
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Inject
@SingleIn(AppScope::class)
class UiPreferences(
    preferenceStore: PreferenceStore,
) {

    val themeMode: Preference<ThemeMode> = preferenceStore.getEnum("pref_theme_mode_key", ThemeMode.SYSTEM)

    val appTheme: Preference<AppTheme> = preferenceStore.getEnum(
        "pref_app_theme",
        if (DeviceUtil.isDynamicColorAvailable) {
            AppTheme.MONET
        } else {
            AppTheme.DEFAULT
        },
    )

    val themeDarkAmoled: Preference<Boolean> = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    val designStyle: Preference<DesignStyle> = preferenceStore.getEnum("pref_design_style", DesignStyle.CLASSIC)

    val modernThemePreset: Preference<ModernThemePreset> = preferenceStore.getEnum(
        "pref_modern_theme_preset",
        ModernThemePreset.LALAPO,
    )

    // Custom v1: opaque RGB HEX; empty string means AUTO (no override).
    val modernCustomPrimary: Preference<String> = preferenceStore.getString("pref_modern_custom_primary", "#485CE0")
    val modernCustomSecondaryOverride: Preference<String> =
        preferenceStore.getString("pref_modern_custom_secondary_override", "")
    val modernCustomTertiaryOverride: Preference<String> =
        preferenceStore.getString("pref_modern_custom_tertiary_override", "")

    // Read only by the formal preference migration; never the active appearance selector.
    internal val legacyModernAppearance: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_modern_appearance",
        false,
    )

    val relativeTime: Preference<Boolean> = preferenceStore.getBoolean("relative_time_v2", true)

    val dateFormat: Preference<String> = preferenceStore.getString("app_date_format", "")

    val tabletUiMode: Preference<TabletUiMode> = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    // AY -->
    val startScreen: Preference<StartScreen> = preferenceStore.getEnum("start_screen", StartScreen.ANIME)
    // <-- AY

    val imagesInDescription: Preference<Boolean> = preferenceStore.getBoolean("pref_render_images_description", true)

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }
    }
}
