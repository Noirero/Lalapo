// AM (DISCORD) -->
package eu.kanade.tachiyomi.data.connections.discord

/**
 * DiscordRPC is a class that implements Discord Rich Presence functionality using WebSockets.
 * @param token discord account token
 */
class DiscordRPC(val token: String, val status: String, var logger: Logger = NoOpLogger) {
    private val discordWebSocket: DiscordWebSocket = DiscordWebSocketImpl(token, logger)
    private var rpc: Presence? = null

    /**
     * Closes the Rich Presence connection.
     */
    fun closeRPC() {
        discordWebSocket.close()
    }

    /**
     * Returns whether the Rich Presence connection is running.
     * @return true if the connection is open, false otherwise.
     */
    private fun isRpcRunning(): Boolean {
        return discordWebSocket.isWebSocketConnected()
    }

    private suspend fun connectToWebSocket() {
        if (token.isEmpty()) {
            logger.e(
                tag = RICH_PRESENCE_TAG,
                event = "Token Seems to be invalid, Please Login if you haven't",
            )
        }
        discordWebSocket.connect()
        rpc?.let { discordWebSocket.sendActivity(it) }
    }

    /**
     * Sets the activity for the Rich Presence.
     * @param activity the activity to set.
     * @param since the activity start time.
     */
    suspend fun updateRPC(
        activity: Activity,
        since: Long? = null,
    ) {
        rpc = Presence(
            activities = listOf(activity),
            afk = true,
            since = since,
            status = status,
        )
        if (!isRpcRunning()) {
            logger.d(
                tag = RICH_PRESENCE_TAG,
                event = "Rpc is currently not running, Trying to connect to a new session",
            )
            connectToWebSocket()
        } else {
            rpc?.let { discordWebSocket.sendActivity(it) }
        }
    }
}
// <-- AM (DISCORD)
