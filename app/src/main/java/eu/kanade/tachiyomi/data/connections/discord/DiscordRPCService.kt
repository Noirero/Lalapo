// AM (DC) -->
package eu.kanade.tachiyomi.data.connections.discord

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.IBinder
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.connections.service.ConnectionsPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import kotlin.math.ceil
import kotlin.math.floor

class DiscordRPCService : Service() {

    private val connectionsManager: ConnectionsManager by injectLazy()

    override fun onCreate() {
        super.onCreate()
        val token = connectionsPreferences.connectionsToken(connectionsManager.discord).get()
        rpc = if (token.isNotBlank()) DiscordRPC(token) else null
        val status = when (connectionsPreferences.discordRPCStatus().get()) {
            -1 -> "dnd"
            0 -> "idle"
            else -> "online"
        }
        if (rpc != null) {
            rpc!!.setApplicationId("952899285983326208")
                .setName("Animiru")
                .setLargeImage("mp:$animiruDiscordImageUrl", "Animiru")
                .setSmallImage("mp:$animiruDiscordImageUrl", "Animiru")
                .setType(0)
                .setStartTimestamps(System.currentTimeMillis())
                .setStatus(status)
                .setButton1("Get the app!", "https://github.com/Quickdesh/Animiru")
                .setButton2("Join the Discord!", "https://discord.gg/yDuHDMwxhv")
                .build()
        } else {
            connectionsPreferences.enableDiscordRPC().set(false)
            return
        }
        notification()
    }

    override fun onDestroy() {
        rpc?.closeRPC()
        rpc = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun notification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "Background Service",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val builder = Notification.Builder(this, CHANNEL)
        builder.setSmallIcon(R.drawable.ic_discord_24dp)
        builder.setContentText("Discord Rpc Running")
        builder.setUsesChronometer(true)
        startForeground(11234, builder.build())
    }

    companion object {
        const val CHANNEL = "Discord RPC"

        internal var rpc: DiscordRPC? = null

        private val connectionsPreferences: ConnectionsPreferences by injectLazy()

        internal lateinit var resources: Resources

        internal var videoInPip = false

        private const val animiruDiscordImageUrl = "attachments/951705840031780865/1005845418405535784/Animiru.png"
        private const val browseDiscordImageUrl = "attachments/951705840031780865/1006843590980415518/browse.png"
        private const val historyDiscordImageUrl = "attachments/951705840031780865/1006843591299178588/history.png"
        private const val libraryDiscordImageUrl = "attachments/951705840031780865/1006843591777341520/library.png"
        private const val moreDiscordImageUrl = "attachments/951705840031780865/1006843592045760533/more.png"
        private const val updatesDiscordImageUrl = "attachments/951705840031780865/1006843592339365888/updates.png"
        private const val videoDiscordImageUrl = "attachments/951705840031780865/1006843592637169714/video.png"
        private const val webviewDiscordImageUrl = "attachments/951705840031780865/1006843593467629568/webview.png"

        internal var lastUsedPage = -1

        internal fun setDiscordPage(pageNumber: Int) {
            lastUsedPage = pageNumber
            if (rpc == null || videoInPip || pageNumber == -1) return

            val largeImage = when (pageNumber) {
                1 -> updatesDiscordImageUrl
                2 -> historyDiscordImageUrl
                3 -> browseDiscordImageUrl
                4 -> moreDiscordImageUrl
                5 -> webviewDiscordImageUrl
                else -> libraryDiscordImageUrl
            }
            val largeText = when (pageNumber) {
                1 -> resources.getString(R.string.label_recent_updates)
                2 -> resources.getString(R.string.label_recent_manga)
                3 -> resources.getString(R.string.browse)
                4 -> resources.getString(R.string.label_settings)
                5 -> resources.getString(R.string.action_web_view)
                else -> resources.getString(R.string.label_library)
            }
            val details = when (pageNumber) {
                1 -> resources.getString(R.string.scrolling)
                2 -> resources.getString(R.string.scrolling)
                3 -> resources.getString(R.string.browsing)
                4 -> resources.getString(R.string.messing)
                5 -> resources.getString(R.string.browsing)
                else -> resources.getString(R.string.browsing)
            }
            val state = when (pageNumber) {
                1 -> resources.getString(R.string.label_recent_updates)
                2 -> resources.getString(R.string.label_recent_manga)
                3 -> resources.getString(R.string.label_sources)
                4 -> resources.getString(R.string.label_settings)
                5 -> resources.getString(R.string.action_web_view)
                else -> resources.getString(R.string.label_library)
            }

            try {
                rpc!!.setLargeImage("mp:$largeImage", largeText)
                    .setType(0)
                    .setDetails(details)
                    .setState(state)
                    .sendData()
            } catch (_: Exception) {}
        }

        internal suspend fun setDiscordVideo(isNsfwSource: Boolean, episodeNumber: Float?, thumbnailUrl: String?, animeTitle: String?) {
            if (rpc == null || episodeNumber == null || thumbnailUrl == null || animeTitle == null) return
            val basePreferences: BasePreferences by injectLazy()

            val discordIncognito = basePreferences.incognitoMode().get() || connectionsPreferences.discordRPCIncognito().get() || isNsfwSource

            val animeName = if (discordIncognito) "" else animeTitle

            val epNum =
                if (discordIncognito) {
                    ""
                } else if (ceil(episodeNumber) == floor(episodeNumber)) {
                    "Episode ${episodeNumber.toInt()}"
                } else {
                    "Episode $episodeNumber"
                }

            withIOContext {
                val networkService: NetworkHelper by injectLazy()
                val client = networkService.client
                var animeThumbnail =
                    if (discordIncognito) {
                        videoDiscordImageUrl
                    } else {
                        try {
                            "external/" +
                                client.newCall(GET("http://140.83.62.114:5000/link?imageLink=$thumbnailUrl"))
                                    .execute()
                                    .header("discord-image-link")
                                    .toString().split("external/")[1]
                        } catch (e: Throwable) {
                            try {
                                "external/" +
                                    client.newCall(GET("http://140.83.62.114:5000/link?imageLink=$thumbnailUrl"))
                                        .execute()
                                        .header("discord-image-link")
                                        .toString().split("external/")[1]
                            } catch (e: Throwable) {
                                videoDiscordImageUrl
                            }
                        }
                    }

                if (animeThumbnail == "external/Not Found") animeThumbnail = videoDiscordImageUrl
                try {
                    rpc!!.setLargeImage("mp:$animeThumbnail", animeName)
                        .setType(3)
                        .setDetails(resources.getString(R.string.watching) + "\n$animeName")
                        .setState(epNum)
                        .sendData()
                } catch (_: Exception) {}
            }
        }
    }
}
// <-- AM (DC)
