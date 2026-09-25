package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.MR
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.presentation.core.i18n.stringResource

object SettingsSyncBackupScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = AMMR.strings.am_label_sync_backup

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(AMMR.strings.am_label_sync_backup),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(AMMR.strings.pref_sync_service_category),
                        onClick = { navigator.push(SettingsSyncmiruScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.label_backup),
                        subtitle = stringResource(MR.strings.pref_backup_summary),
                        onClick = { navigator.push(SettingsDataScreen) },
                    ),
                ),
            ),
        )
    }
}
