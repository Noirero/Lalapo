// AM (DISCORD) -->
package eu.kanade.tachiyomi.data.connections.discord

import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.BaseConnection

class Discord(id: Long) : BaseConnection(
    id,
    "Discord",
) {
    override fun getLogo() = R.drawable.ic_discord_24dp

    override fun getLogoColor() = Color.rgb(88, 101, 242)

    override fun logout() {
        super.logout()
        connectionsPreferences.connectionsToken(this).delete()
    }

    override suspend fun login(username: String, password: String) {
        // Not Needed
    }
}
// <-- AM (DISCORD)
