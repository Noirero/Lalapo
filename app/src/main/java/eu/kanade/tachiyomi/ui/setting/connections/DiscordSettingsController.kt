// AM -->
package eu.kanade.tachiyomi.ui.setting.connections

import android.content.Intent
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.connections.discord.DiscordLoginActivity
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.util.preference.bindTo
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes

class DiscordSettingsController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.pref_category_discord

        switchPreference {
            bindTo(preferences.enableDiscordRPC())
            titleRes = R.string.pref_enable_discord_rpc
            defaultValue = false
            onClick { discordLogin() }
        }
    }

    private fun discordLogin() {
        val intent = Intent(activity!!, DiscordLoginActivity::class.java)
        startActivity(intent)
    }
}
// AM <--
