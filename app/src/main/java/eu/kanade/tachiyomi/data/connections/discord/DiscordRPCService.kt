// AM -->
package eu.kanade.tachiyomi.data.connections.discord

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.IBinder
import android.util.Log
import com.my.discordrpc.DiscordRPC
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.launchUI
import uy.kohesive.injekt.injectLazy

class DiscordRPCService : Service() {

    private var context: Context? = null

    override fun onCreate() {
        super.onCreate()
        context = this
        rpc!!.setStartTimestamps(System.currentTimeMillis())
        rpc!!.build()
        isBuilt = true
    }

    override fun onDestroy() {
        launchUI {
            rpc!!.closeRPC()
            isBuilt = false
        }
        Log.i("RPCYO", "ITS DESTROYED BRUV")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        internal var isBuilt = false
        internal var isPip = false

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

        fun setDRPC(type: String?, resources: Resources, image: String? = null, imageText: String? = null, details: String? = null, state: String? = null) {
            if (isPip) return
            if (preferences.enableDiscordRPC().get()) launchUI {
                if (type == null) {
                    val useMp = image!!.startsWith("attachments/951705840031780865")
                    rpc!!.setLargeImage(image, imageText!!, useMp)
                        .setDetails(details)
                        .setState(state)
                        .sendData()
                }
                when (type) {
                    "library" -> setDRPC(null, resources, library, resources.getString(R.string.label_animelib), resources.getString(R.string.browsing), resources.getString(R.string.label_animelib))
                    "browse" -> setDRPC(null, resources, browse, resources.getString(R.string.browse), resources.getString(R.string.browsing), resources.getString(R.string.sources))
                    "more" -> setDRPC(null, resources, more, resources.getString(R.string.label_more), resources.getString(R.string.messing), resources.getString(R.string.settings))
                    "history" -> setDRPC(null, resources, history, resources.getString(R.string.label_recent_history), resources.getString(R.string.scrolling), resources.getString(R.string.label_recent_history))
                    "updates" -> setDRPC(null, resources, updates, resources.getString(R.string.label_recent_updates), resources.getString(R.string.scrolling), resources.getString(R.string.label_recent_updates))
                    "animiru" -> setDRPC(null, resources, animiru, resources.getString(R.string.app_name))
                }
            }
        }

        internal var rpc: DiscordRPC? = DiscordRPC(token)
            .setApplicationId("962990036020756480")
            .setName("Animiru")
            .setLargeImage(animiru, "Animiru", true)
            .setSmallImage(animiru, "Animiru", true)
            .setType(0)
            .setButton2("Get the app!", "https://github.com/Quickdesh/Animiru")
    }
}
// AM <--
