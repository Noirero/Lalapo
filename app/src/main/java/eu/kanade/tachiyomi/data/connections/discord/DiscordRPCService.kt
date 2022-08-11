// AM -->
package eu.kanade.tachiyomi.data.connections.discord

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.my.discordrpc.DiscordRPC
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.launchUI
import uy.kohesive.injekt.injectLazy

class DiscordRPCService : Service() {

    private var context: Context? = null

    override fun onCreate() {
        super.onCreate()
        context = this
        rpc!!.build()
    }

    override fun onDestroy() {
        launchUI { rpc!!.closeRPC() }
        Log.i("RPCYO", "ITS DESTROYED BRUV")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private val preferences: PreferencesHelper by injectLazy()

        private val connectionsManager: ConnectionsManager by injectLazy()

        private val token = preferences.connectionToken(connectionsManager.discord).get()

        internal const val animiru = "attachments/951705840031780865/1005845418405535784/Animiru.png"
        internal const val browse = "attachments/951705840031780865/1006843590980415518/browse.png"
        internal const val history = "attachments/951705840031780865/1006843591299178588/history.png"
        internal const val library = "attachments/951705840031780865/1006843591777341520/library.png"
        internal const val more = "attachments/951705840031780865/1006843592045760533/more.png"
        internal const val updates = "attachments/951705840031780865/1006843592339365888/updates.png"
        internal const val video = "attachments/951705840031780865/1006843592637169714/video.png"
        internal const val webview = "attachments/951705840031780865/1006843593467629568/webview.png"

        fun setDRPC(image: String, imageText: String, details: String?, state: String?) {
            if (preferences.enableDiscordRPC().get()) launchUI {
                rpc!!.setLargeImage(
                    image,
                    imageText,
                )
                    .setDetails(details)
                    .setState(state)
                    .sendData()
            }
        }

        internal var rpc: DiscordRPC? = DiscordRPC(token)
            .setApplicationId("962990036020756480")
            .setName("Animiru")
            .setLargeImage(animiru, "Animiru")
            .setSmallImage(animiru, "Animiru")
            .setStartTimestamps(System.currentTimeMillis())
            .setType(0)
            .setButton2("Get the app!", "https://github.com/Quickdesh/Animiru")
    }
}
// AM <--
