// AM -->
package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import eu.kanade.presentation.more.settings.SettingsMainScreen
import eu.kanade.presentation.more.settings.SettingsSection
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.connections.discord.DiscordLoginActivity
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.BasicComposeController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.setting.connections.DiscordSettingsController
import uy.kohesive.injekt.injectLazy

class SettingsConnectionsController : BasicComposeController() {

    private val preferences: PreferencesHelper by injectLazy()

    override fun getTitle() = resources?.getString(R.string.pref_category_connections)

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        val settingsSections = listOf(
            SettingsSection(
                titleRes = R.string.pref_category_discord,
                painter = rememberResourceBitmapPainter(R.drawable.ic_discord_24dp),
                onClick = {
                    if (preferences.discordToken().get() == "") discordLogin()
                    else router.pushController(DiscordSettingsController())
                },
                useIconColor = true,
                iconColor = "#5865f2",
            ),
        )

        SettingsMainScreen(
            nestedScrollInterop = nestedScrollInterop,
            sections = settingsSections,
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_main, menu)

        // Initialize search option.
        val searchItem = menu.findItem(R.id.action_search)
        searchItem.isVisible = false
    }

    private fun discordLogin() {
        val intent = Intent(activity!!, DiscordLoginActivity::class.java)
        startActivity(intent)
    }
}
// AM <--
