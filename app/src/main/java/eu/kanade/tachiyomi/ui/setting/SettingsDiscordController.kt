// AM -->
package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.data.connections.ConnectionsService
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.ui.setting.connections.ConnectionsLogoutDialog
import eu.kanade.tachiyomi.util.preference.asImmediateFlow
import eu.kanade.tachiyomi.util.preference.bindTo
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import uy.kohesive.injekt.injectLazy

class SettingsDiscordController : SettingsController(), ConnectionsLogoutDialog.Listener {

    private val connectionsManager: ConnectionsManager by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.connection_discord

        switchPreference {
            bindTo(preferences.enableDiscordRPC())
            titleRes = R.string.pref_enable_discord_rpc
            defaultValue = false
        }
        preferences.enableDiscordRPC()
            .asImmediateFlow {
                if (it) activity!!.startService(Intent(activity!!, DiscordRPCService::class.java))
                else activity!!.stopService(Intent(activity!!, DiscordRPCService::class.java))
            }

        preference {
            key = "logout_discord"
            titleRes = R.string.logout
            onClick {
                val dialog = ConnectionsLogoutDialog(connectionsManager.discord)
                dialog.targetController = this@SettingsDiscordController
                dialog.showDialog(router)
            }
        }
    }

    override fun connectionsLogoutDialogClosed(service: ConnectionsService) {
        preferences.enableDiscordRPC().set(false)
    }
}
// AM <--
