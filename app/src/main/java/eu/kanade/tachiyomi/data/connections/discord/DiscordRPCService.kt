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
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.i18n.stringResource
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.model.Category.Companion.UNCATEGORIZED_ID
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

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
            setContentText(context.stringResource(MR.strings.pref_discord_rpc))
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

        fun stop(context: Context, delay: Long = 30000L) {
            handler.postDelayed(
                { context.stopService(Intent(context, DiscordRPCService::class.java)) },
                delay,
            )
        }

        private var since = 0L

        internal var lastUsedScreen = DiscordScreen.APP
            set(value) {
                field = if (value == DiscordScreen.VIDEO || value == DiscordScreen.WEBVIEW) field else value
            }

        internal suspend fun setScreen(
            context: Context,
            discordScreen: DiscordScreen,
            playerData: PlayerData = PlayerData(),
        ) {
            if (PipState.mode == PipState.ON && discordScreen != DiscordScreen.VIDEO) return
            lastUsedScreen = discordScreen

            if (rpc == null) return

            val name = playerData.animeTitle ?: context.stringResource(MR.strings.app_name)

            val details = playerData.animeTitle ?: context.stringResource(discordScreen.details)

            val state = playerData.episodeNumber ?: context.stringResource(discordScreen.text)

            val imageUrl = playerData.thumbnailUrl ?: discordScreen.imageUrl

            rpc!!.updateRPC(
                activity = Activity(
                    name = name,
                    details = details,
                    state = state,
                    type = 3,
                    timestamps = Activity.Timestamps(start = since),
                    assets = Activity.Assets(
                        largeImage = "mp:$imageUrl",
                        smallImage = "mp:${DiscordScreen.APP.imageUrl}",
                        smallText = context.stringResource(DiscordScreen.APP.text),
                    ),
                ),
                since = since,
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
                        // Thanks to https://github.com/dead8309/Kizzy
                        client.newCall(GET("https://kizzy-api.vercel.app/image?url=${playerData.thumbnailUrl}")).execute()
                    } catch (e: Throwable) {
                        null
                    }
                } else {
                    null
                }

                val animeThumbnail = response?.body?.string()
                    ?.takeIf { !it.contains("external/Not Found") }
                    ?.substringAfter("\"id\": \"")?.substringBefore("\"}")
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
