// AM (CN) -->
package eu.kanade.tachiyomi.data.connections

import android.content.Context
import eu.kanade.tachiyomi.data.connections.discord.Discord

class ConnectionsManager(context: Context) {

    companion object {
        const val DISCORD = 201L
    }

    val discord = Discord(context, DISCORD)

    val services = listOf(discord)

    fun getService(id: Long) = services.find { it.id == id }
}
// AM (CN) <--
