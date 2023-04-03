// AM (DC) -->
package eu.kanade.tachiyomi.data.connections.discord

import android.util.ArrayMap
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.lang.RuntimeException
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import javax.net.ssl.SSLParameters

class DiscordRPC(var token: String) {
    private var applicationId: String? = null
    private var activityName: String? = null
    private var details: String? = null
    private var state: String? = null
    private var largeImage: String? = null
    private var smallImage: String? = null
    private var largeText: String? = null
    private var smallText: String? = null
    private var status: String? = null
    private var startTimestamps: Long? = null
    private var stopTimestamps: Long? = null
    private var type = 0
    var rpc = ArrayMap<String, Any>()
    var webSocketClient: WebSocketClient? = null
    var gson: Gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    var heartbeatRunnable: Runnable
    var heartbeatThread: Thread? = null
    var heartbeatInterval = 0
    var seq = 0

    private var sessionId: String? = null
    private var reconnectSession = false
    private var buttons = ArrayList<String>()
    private var buttonUrl = ArrayList<String>()

    fun closeRPC() {
        if (heartbeatThread != null && !heartbeatThread!!.isInterrupted) heartbeatThread!!.interrupt()
        if (webSocketClient != null) webSocketClient!!.close(1000)
    }

    /**
     * Application Id for Rpc
     * An application id is required for functioning of urls in buttons
     * @param applicationId
     * @return
     */
    fun setApplicationId(applicationId: String?): DiscordRPC {
        this.applicationId = applicationId
        return this
    }

    /**
     * Activity Name of Rpc
     *
     * @param activity_name
     * @return
     */
    fun setName(activity_name: String?): DiscordRPC {
        this.activityName = activity_name
        return this
    }

    /**
     * Details of Rpc
     *
     * @param details
     * @return
     */
    fun setDetails(details: String?): DiscordRPC {
        this.details = details
        return this
    }

    /**
     * Rpc State
     *
     * @param state
     * @return
     */
    fun setState(state: String?): DiscordRPC {
        this.state = state
        return this
    }

    /**
     * Large image on rpc
     * How to get Image ?
     * Upload image to any discord chat and copy its media link it should look like "https://media.discordapp.net/attachments/90202992002/xyz.png" now just use the image link from attachments part
     * so it would look like: .setLargeImage("attachments/90202992002/xyz.png")
     * @param largeImage
     * @return
     */
    fun setLargeImage(largeImage: String?, largeText: String?): DiscordRPC {
        this.largeImage = largeImage
        this.largeText = largeText
        return this
    }

    /**
     * Small image on Rpc
     *
     * @param smallImage
     * @return
     */
    fun setSmallImage(smallImage: String?, smallText: String?): DiscordRPC {
        this.smallImage = smallImage
        this.smallText = smallText
        return this
    }

    /**
     * start timestamps
     *
     * @param startTimestamps
     * @return
     */
    fun setStartTimestamps(startTimestamps: Long?): DiscordRPC {
        this.startTimestamps = startTimestamps
        return this
    }

    /**
     * stop timestamps
     *
     * @param stopTimestamps
     * @return
     */
    fun setStopTimestamps(stopTimestamps: Long?): DiscordRPC {
        this.stopTimestamps = stopTimestamps
        return this
    }

    /**
     * Activity Types
     * 0: Playing
     * 1: Streaming
     * 2: Listening
     * 3: Watching
     * 5: Competing
     *
     * @param type
     * @return
     */
    fun setType(type: Int): DiscordRPC {
        this.type = type
        return this
    }

    /**
     * Status type for profile online,idle,dnd
     *
     * @param status
     * @return
     */
    fun setStatus(status: String?): DiscordRPC {
        this.status = status
        return this
    }

    /**
     * Button1 text
     * @param buttonLabel
     * @return
     */
    fun setButton1(buttonLabel: String, link: String): DiscordRPC {
        buttons.add(buttonLabel)
        buttonUrl.add(link)
        return this
    }

    /**
     * Button2 text
     * @param buttonLabel
     * @return
     */
    fun setButton2(buttonLabel: String, link: String): DiscordRPC {
        buttons.add(buttonLabel)
        buttonUrl.add(link)
        return this
    }

    fun build() {
        val presence = ArrayMap<String, Any?>()
        val activity = ArrayMap<String, Any?>()
        activity["application_id"] = applicationId
        activity["name"] = activityName
        activity["details"] = details
        activity["state"] = state
        activity["type"] = type
        val timestamps = ArrayMap<String, Any?>()
        timestamps["start"] = startTimestamps
        timestamps["stop"] = stopTimestamps
        activity["timestamps"] = timestamps
        val assets = ArrayMap<String, Any?>()
        assets["large_image"] = largeImage
        assets["small_image"] = smallImage
        assets["large_text"] = largeText
        assets["small_text"] = smallText
        activity["assets"] = assets
        if (buttons.size > 0) {
            val metadata = ArrayMap<String, Any>()
            activity["buttons"] = buttons
            metadata["button_urls"] = buttonUrl
            activity["metadata"] = metadata
        }
        presence["activities"] = arrayOf<Any>(activity)
        presence["afk"] = true
        presence["since"] = startTimestamps
        presence["status"] = status
        rpc["op"] = 3
        rpc["d"] = presence
        createWebsocketClient()
    }

    fun sendData() {
        val presence = ArrayMap<String, Any?>()
        val activity = ArrayMap<String, Any?>()
        activity["application_id"] = applicationId
        activity["name"] = activityName
        activity["details"] = details
        activity["state"] = state
        activity["type"] = type
        val timestamps = ArrayMap<String, Any?>()
        timestamps["start"] = startTimestamps
        timestamps["stop"] = stopTimestamps
        activity["timestamps"] = timestamps
        val assets = ArrayMap<String, Any?>()
        assets["large_image"] = largeImage
        assets["small_image"] = smallImage
        assets["large_text"] = largeText
        assets["small_text"] = smallText
        activity["assets"] = assets
        if (buttons.size > 0) {
            val metadata = ArrayMap<String, Any>()
            activity["buttons"] = buttons
            metadata["button_urls"] = buttonUrl
            activity["metadata"] = metadata
        }
        presence["activities"] = arrayOf<Any>(activity)
        presence["afk"] = true
        presence["since"] = startTimestamps
        presence["status"] = status
        rpc["op"] = 3
        rpc["d"] = presence
        webSocketClient?.send(gson.toJson(rpc))
    }

    fun sendIdentify() {
        val prop = ArrayMap<String, Any>()
        prop["\$os"] = "windows"
        prop["\$browser"] = "Chrome"
        prop["\$device"] = "disco"
        val data = ArrayMap<String, Any>()
        data["token"] = token
        data["properties"] = prop
        data["compress"] = false
        data["intents"] = 0
        val identify = ArrayMap<String, Any>()
        identify["op"] = 2
        identify["d"] = data
        webSocketClient!!.send(gson.toJson(identify))
    }

    private fun createWebsocketClient() {
        Log.i("Connecting", "")
        val uri: URI = try {
            URI("wss://gateway.discord.gg/?encoding=json&v=9")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }
        val headerMap = ArrayMap<String, String>()
        webSocketClient = object : WebSocketClient(uri, headerMap) {
            override fun onOpen(s: ServerHandshake) {
                Log.e("Connection opened", "")
            }

            override fun onMessage(message: String) {
                val map = gson.fromJson<ArrayMap<String, Any>>(
                    message,
                    object : TypeToken<ArrayMap<String?, Any?>?>() {}.type,
                )
                val o = map["s"]
                if (o != null) {
                    seq = (o as Double).toInt()
                }
                when ((map["op"] as Double?)!!.toInt()) {
                    0 -> if (map["t"] as String? == "READY") {
                        sessionId = (map["d"] as Map<*, *>?)!!["session_id"].toString()
                        Log.e("Connected", "")
                        webSocketClient!!.send(gson.toJson(rpc))
                        Log.e("", sessionId!!)
                        return
                    }
                    10 -> if (!reconnectSession) {
                        val data = map["d"] as Map<*, *>?
                        heartbeatInterval = (data!!["heartbeat_interval"] as Double?)!!.toInt()
                        heartbeatThread = Thread(heartbeatRunnable)
                        heartbeatThread!!.start()
                        sendIdentify()
                    } else {
                        Log.e("Sending Reconnect", "")
                        val data = map["d"] as Map<*, *>?
                        heartbeatInterval = (data!!["heartbeat_interval"] as Double?)!!.toInt()
                        heartbeatThread = Thread(heartbeatRunnable)
                        heartbeatThread!!.start()
                        reconnectSession = false
                        webSocketClient!!.send("{\"op\": 6,\"d\":{\"token\":\"$token\",\"session_id\":\"$sessionId\",\"seq\":$seq}}")
                    }
                    1 -> {
                        if (!Thread.interrupted()) {
                            heartbeatThread!!.interrupt()
                        }
                        webSocketClient!!.send("{\"op\":1, \"d\":" + (if (seq == 0) "null" else seq.toString()) + "}")
                    }
                    11 -> {
                        if (!Thread.interrupted()) {
                            heartbeatThread!!.interrupt()
                        }
                        heartbeatThread = Thread(heartbeatRunnable)
                        heartbeatThread!!.start()
                    }
                    7 -> {
                        reconnectSession = true
                        webSocketClient!!.close(4000)
                    }
                    9 -> if (!heartbeatThread!!.isInterrupted) {
                        heartbeatThread!!.interrupt()
                        heartbeatThread = Thread(heartbeatRunnable)
                        heartbeatThread!!.start()
                        sendIdentify()
                    }
                }
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                if (code == 4000) {
                    reconnectSession = true
                    heartbeatThread!!.interrupt()
                    Log.e("", "Closed Socket")
                    val newTh = Thread {
                        try {
                            Thread.sleep(200)
                            reconnect()
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    newTh.start()
                } else {
                    throw RuntimeException("Invalid")
                }
            }

            override fun onError(e: Exception) {
                if (e.message != "Interrupt") {
                    closeRPC()
                }
            }

            override fun onSetSSLParameters(p: SSLParameters) {
                try {
                    super.onSetSSLParameters(p)
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }
        }
        (webSocketClient as WebSocketClient).connect()
    }

    init {
        heartbeatRunnable = Runnable {
            try {
                if (heartbeatInterval < 10000) throw RuntimeException("invalid")
                Thread.sleep(heartbeatInterval.toLong())
                webSocketClient!!.send("{\"op\":1, \"d\":" + (if (seq == 0) "null" else seq.toString()) + "}")
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}
// <-- AM (DC)
