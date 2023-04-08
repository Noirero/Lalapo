// AM (DC) -->
package eu.kanade.presentation.more.settings.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import eu.kanade.domain.connections.service.ConnectionsPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsDiscordScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    @StringRes
    override fun getTitleRes() = R.string.pref_category_connections

    @Composable
    override fun RowScope.AppBarAction() {
        val uriHandler = LocalUriHandler.current
        IconButton(onClick = { uriHandler.openUri("https://tachiyomi.org/help/guides/tracking/") }) {
            Icon(
                imageVector = Icons.Outlined.HelpOutline,
                contentDescription = stringResource(R.string.tracking_guide),
            )
        }
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val connectionsPreferences = remember { Injekt.get<ConnectionsPreferences>() }
        val connectionsManager = remember { Injekt.get<ConnectionsManager>() }
        val enableDRPCPref = connectionsPreferences.enableDiscordRPC()
        val discordRPCStatus = connectionsPreferences.discordRPCStatus()
        val discordRPCIncognito = connectionsPreferences.discordRPCIncognito()

        val enableDRPC by enableDRPCPref.collectAsState()

        var dialog by remember { mutableStateOf<Any?>(null) }
        dialog?.run {
            when (this) {
                is LogoutConnectionsDialog -> {
                    ConnectionsLogoutDialog(
                        service = service,
                        onDismissRequest = {
                            dialog = null
                            enableDRPCPref.set(false)
                        },
                    )
                }
            }
        }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(R.string.connections_discord),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = enableDRPCPref,
                        title = stringResource(R.string.pref_enable_discord_rpc),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = discordRPCStatus,
                        title = stringResource(R.string.pref_discord_status),
                        entries = mapOf(
                            -1 to stringResource(R.string.pref_discord_dnd),
                            0 to stringResource(R.string.pref_discord_idle),
                            1 to stringResource(R.string.pref_discord_online),
                        ),
                        enabled = enableDRPC,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = discordRPCIncognito,
                        title = stringResource(R.string.pref_discord_incognito),
                        subtitle = stringResource(R.string.pref_discord_incognito_summary),
                        enabled = enableDRPC,
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.logout),
                        onClick = { dialog = LogoutConnectionsDialog(connectionsManager.discord) },
                    ),
                ),
            ),
        )
    }
}
// <-- AM (DC)
