// AM (DISCORD) -->
package eu.kanade.tachiyomi.data.connections.discord

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.ui.util.fastAny
import eu.kanade.domain.connections.service.ConnectionsPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.player.viewer.PipState
import eu.kanade.tachiyomi.util.system.notificationBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.model.Category.Companion.UNCATEGORIZED_ID
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.ceil
import kotlin.math.floor

class DiscordRPCService : Service() {

    private val connectionsManager: ConnectionsManager by injectLazy()

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        val token = connectionsPreferences.connectionsToken(connectionsManager.discord).get()
        val status = when (connectionsPreferences.discordRPCStatus().get()) {
            -1 -> "dnd"
            0 -> "idle"
            else -> "online"
        }
        rpc = if (token.isNotBlank()) DiscordRPC(token, status) else null
        if (rpc != null) {
            launchIO { setScreen(this@DiscordRPCService, lastUsedScreen) }
            notification(this)
        } else {
            connectionsPreferences.enableDiscordRPC().set(false)
        }
    }

    override fun onDestroy() {
        NotificationReceiver.dismissNotification(this, Notifications.ID_DISCORD_RPC)
        rpc?.closeRPC()
        rpc = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun notification(context: Context) {
        val builder = context.notificationBuilder(Notifications.CHANNEL_DISCORD_RPC) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setSmallIcon(R.drawable.ic_discord_24dp)
            setContentText(context.resources.getString(R.string.pref_discord_rpc))
            setAutoCancel(false)
            setOngoing(true)
            setUsesChronometer(true)
        }

        startForeground(Notifications.ID_DISCORD_RPC, builder.build())
    }

    companion object {

        private val connectionsPreferences: ConnectionsPreferences by injectLazy()

        internal var rpc: DiscordRPC? = null

        private val handler = Handler(Looper.getMainLooper())

        fun start(context: Context) {
            handler.removeCallbacksAndMessages(null)
            if (rpc == null && connectionsPreferences.enableDiscordRPC().get()) {
                since = System.currentTimeMillis()
                context.startService(Intent(context, DiscordRPCService::class.java))
            }
        }

        fun stop(context: Context, delay: Long = 60000L) {
            handler.postDelayed(
                { context.stopService(Intent(context, DiscordRPCService::class.java)) },
                delay,
            )
        }

        private var since = 0L

        internal var lastUsedScreen = DiscordScreen.APP
            set(value) {
                field = if (value != DiscordScreen.VIDEO || value != DiscordScreen.WEBVIEW) value else field
            }

        internal suspend fun setScreen(
            context: Context,
            discordScreen: DiscordScreen,
            playerData: PlayerData = PlayerData(),
        ) {
            if (rpc == null) return

            if (PipState.mode == PipState.ON && discordScreen != DiscordScreen.VIDEO) return
            lastUsedScreen = discordScreen

            val details = playerData.animeTitle ?: context.resources.getString(discordScreen.details)

            val state = playerData.episodeNumber ?: context.resources.getString(
                if (discordScreen == DiscordScreen.BROWSE) R.string.label_sources else discordScreen.text,
            )

            val text = playerData.animeTitle ?: context.resources.getString(discordScreen.text)

            val imageUrl = playerData.thumbnailUrl ?: discordScreen.imageUrl

            rpc!!.updateRPC(
                activity = Activity(
                    name = context.resources.getString(R.string.app_name),
                    state = state,
                    details = details,
                    type = 0,
                    timestamps = Timestamps(start = since),
                    assets = Assets(
                        largeImage = "mp:$imageUrl",
                        smallImage = "mp:${DiscordScreen.APP.imageUrl}",
                        largeText = text,
                        smallText = context.resources.getString(DiscordScreen.APP.text),
                    ),
                ),
            )
        }

        internal suspend fun setPlayerActivity(context: Context, playerData: PlayerData = PlayerData()) {
            if (rpc == null || playerData.thumbnailUrl == null || playerData.animeId == null) return

            val animeCategoryIds = Injekt.get<GetAnimeCategories>()
                .await(playerData.animeId)
                .map { it.id.toString() }
                .run { ifEmpty { plus(UNCATEGORIZED_ID.toString()) } }

            val discordIncognitoMode = connectionsPreferences.discordRPCIncognito().get()
            val incognitoCategories = connectionsPreferences.discordRPCIncognitoCategories().get()

            val incognitoCategory = animeCategoryIds.fastAny {
                it in incognitoCategories
            }

            val discordIncognito = discordIncognitoMode || playerData.incognitoMode || incognitoCategory

            val animeTitle = playerData.animeTitle.takeUnless { discordIncognito }

            val episodeNumber = playerData.episodeNumber?.toFloatOrNull()?.let {
                when {
                    discordIncognito -> null
                    ceil(it) == floor(it) -> "Episode ${it.toInt()}"
                    else -> "Episode $it"
                }
            }

            withIOContext {
                val networkService: NetworkHelper by injectLazy()
                val client = networkService.client
                val response = if (!discordIncognito) {
                    try {
                        client.newCall(GET("http://140.83.62.114:5000/link?imageLink=${playerData.thumbnailUrl}")).execute()
                    } catch (e: Throwable) {
                        null
                    }
                } else {
                    null
                }

                val animeThumbnail = response?.header("discord-image-link")
                    ?.takeIf { it != "external/Not Found" }
                    ?.split("external/")?.getOrNull(1)?.let { "external/$it" }

                setScreen(
                    context = context,
                    discordScreen = DiscordScreen.VIDEO,
                    playerData = PlayerData(
                        animeTitle = animeTitle,
                        episodeNumber = episodeNumber,
                        thumbnailUrl = animeThumbnail,
                    ),
                )
            }
        }
    }
}
// <-- AM (DISCORD)
