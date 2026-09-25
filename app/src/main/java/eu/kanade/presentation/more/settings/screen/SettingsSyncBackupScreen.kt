package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import mihon.app.di.appGraph
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
        val context = LocalContext.current
        val connectionManager = remember { context.appGraph.connectionManager }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(AMMR.strings.am_label_sync_backup),
                preferenceItems = listOf(
                    Preference.PreferenceItem.ConnectionPreference(
                        title = connectionManager.syncmiru.name,
                        connection = connectionManager.syncmiru,
                        login = { navigator.push(SettingsSyncmiruScreen) },
                        openSettings = { navigator.push(SettingsSyncmiruScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.label_backup),
                        subtitle = stringResource(MR.strings.pref_backup_summary),
                        onClick = { navigator.push(SettingsBackupScreen) },
                    ),
                ),
            ),
        )
    }
}
