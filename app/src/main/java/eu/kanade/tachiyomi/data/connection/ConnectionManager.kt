// AM (CONNECTION) -->
package eu.kanade.tachiyomi.data.connection

import eu.kanade.tachiyomi.data.connection.discord.Discord

class ConnectionManager {

    companion object {
        const val DISCORD = 201L
    }

    // AM (DISCORD) -->
    val discord = Discord(DISCORD)
    // <-- AM (DISCORD)

    val services: List<BaseConnection> = listOf(discord)
}
// <-- AM (CONNECTION)
