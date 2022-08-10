// AM -->
package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.util.preference.bindTo
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.injectLazy

class SettingsDiscordController : SettingsController() {

    private val connectionsManager: ConnectionsManager by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.connection_discord

        switchPreference {
            bindTo(preferences.enableDiscordRPC())
            titleRes = R.string.pref_enable_discord_rpc
            defaultValue = false
        }

        preference {
            key = "logout_discord"
            titleRes = R.string.logout
            onClick {
                connectionsManager.discord.logout()
                view?.context?.toast(R.string.logout_success)
                activity!!.onBackPressed()
                preferences.enableDiscordRPC().set(false)
            }
        }
    }
}
// AM <--
