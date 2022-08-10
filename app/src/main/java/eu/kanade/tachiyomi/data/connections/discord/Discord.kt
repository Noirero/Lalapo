// AM -->
package eu.kanade.tachiyomi.data.connections.discord

import android.content.Context
import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionService

class Discord(private val context: Context, id: Long) : ConnectionService(id) {
    @StringRes
    override fun nameRes() = R.string.connection_discord

    override fun getLogo() = R.drawable.ic_discord_24dp

    override fun getLogoColor() = Color.rgb(88, 101, 242)

    override fun logout() {
        super.logout()
        preferences.connectionToken(this).delete()
    }
}
// AM <--
