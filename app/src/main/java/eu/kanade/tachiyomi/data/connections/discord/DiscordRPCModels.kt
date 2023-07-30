// AM (DISCORD) -->

/*
 *   Copyright (c) 2023 Kizzy. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

// Library from https://github.com/dead8309/KizzyRPC (Thank you)
package eu.kanade.tachiyomi.data.connections.discord

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement

// Constant for Rich Presence operation code
const val RICH_PRESENCE_OP_CODE = 3

// Constant for logging tag
const val RICH_PRESENCE_TAG = "discord_rpc"

// Constant for application id
private const val RICH_PRESENCE_APPLICATION_ID = "952899285983326208"

// Constant for buttons list
private val RICH_PRESENCE_BUTTONS = listOf("Get the app!", "Join the Discord!")

// Constant for metadata list
private val RICH_PRESENCE_METADATA = Metadata(
    listOf(
        "https://github.com/Quickdesh/Animiru",
        "https://discord.gg/yDuHDMwxhv",
    ),
)

@Serializable
data class Activity(
    @SerialName("name")
    val name: String?,
    @SerialName("state")
    val state: String? = null,
    @SerialName("details")
    val details: String? = null,
    @SerialName("type")
    val type: Int? = 0,
    @SerialName("timestamps")
    val timestamps: Timestamps? = null,
    @SerialName("assets")
    val assets: Assets? = null,
    @SerialName("buttons")
    val buttons: List<String?>? = RICH_PRESENCE_BUTTONS,
    @SerialName("metadata")
    val metadata: Metadata? = RICH_PRESENCE_METADATA,
    @SerialName("application_id")
    val applicationId: String? = RICH_PRESENCE_APPLICATION_ID,
    @SerialName("url")
    val url: String? = null,
)

@Serializable
data class Assets(
    @SerialName("large_image")
    val largeImage: String?,
    @SerialName("small_image")
    val smallImage: String?,
    @SerialName("large_text")
    val largeText: String? = null,
    @SerialName("small_text")
    val smallText: String? = null,
)

@Serializable
data class Presence(
    @SerialName("activities")
    val activities: List<Activity?>?,
    @SerialName("afk")
    val afk: Boolean? = true,
    @SerialName("since")
    val since: Long? = null,
    @SerialName("status")
    val status: String? = null,
)

@Serializable
data class Heartbeat(
    @SerialName("heartbeat_interval")
    val heartbeatInterval: Long,
)

@Serializable
data class Identify(
    @SerialName("capabilities")
    val capabilities: Int,
    @SerialName("compress")
    val compress: Boolean,
    @SerialName("largeThreshold")
    val largeThreshold: Int,
    @SerialName("properties")
    val properties: Properties,
    @SerialName("token")
    val token: String,
) {
    companion object {
        fun String.toIdentifyPayload() = Identify(
            capabilities = 65,
            compress = false,
            largeThreshold = 100,
            properties = Properties(
                browser = "Discord Client",
                device = "ktor",
                os = "Windows",
            ),
            token = this,
        )
    }
}

@Serializable
data class Properties(
    @SerialName("browser")
    val browser: String,
    @SerialName("device")
    val device: String,
    @SerialName("os")
    val os: String,
)

@Serializable
data class Metadata(
    @SerialName("button_urls")
    val buttonUrls: List<String?>?,
)

@Serializable
data class Ready(
    @SerialName("resume_gateway_url")
    val resumeGatewayUrl: String? = null,
    @SerialName("session_id")
    val sessionId: String? = null,
)

@Serializable
data class Resume(
    @SerialName("seq")
    val seq: Int,
    @SerialName("session_id")
    val sessionId: String?,
    @SerialName("token")
    val token: String,
)

@Serializable
data class Payload(
    @SerialName("t")
    val t: String? = null,
    @SerialName("s")
    val s: Int? = null,
    @SerialName("op")
    val op: OpCode? = null,
    @SerialName("d")
    val d: JsonElement? = null,
)

@Serializable
data class Timestamps(
    @SerialName("end")
    val end: Long? = null,
    @SerialName("start")
    val start: Long? = null,
)

@Serializable(OpCodeSerializer::class)
enum class OpCode(val value: Int) {
    /** An event was dispatched. */
    DISPATCH(0),

    /** Fired periodically by the client to keep the connection alive. */
    HEARTBEAT(1),

    /** Starts a new session during the initial handshake. */
    IDENTIFY(2),

    /** Update the client's presence. */
    PRESENCE_UPDATE(3),

    /** Joins/leaves or moves between voice channels. */
    VOICE_STATE(4),

    /** Resume a previous session that was disconnected. */
    RESUME(6),

    /** You should attempt to reconnect and resume immediately. */
    RECONNECT(7),

    /** Request information about offline guild members in a large guild. */
    REQUEST_GUILD_MEMBERS(8),

    /** The session has been invalidated. You should reconnect and identify/resume accordingly */
    INVALID_SESSION(9),

    /** Sent immediately after connecting, contains the heartbeat_interval to use. */
    HELLO(10),

    /** Sent in response to receiving a heartbeat to acknowledge that it has been received. */
    HEARTBEAT_ACK(11),

    /** For future use or unknown opcodes. */
    UNKNOWN(-1),
    ;
}

class OpCodeSerializer : KSerializer<OpCode> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("OpCode", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): OpCode {
        val opCode = decoder.decodeInt()
        return OpCode.values().firstOrNull { it.value == opCode } ?: throw IllegalArgumentException("Unknown OpCode $opCode")
    }

    override fun serialize(encoder: Encoder, value: OpCode) {
        encoder.encodeInt(value.value)
    }
}

interface Logger {
    fun clear()
    fun i(tag: String, event: String)
    fun e(tag: String, event: String)
    fun d(tag: String, event: String)
    fun w(tag: String, event: String)
}

object NoOpLogger : Logger {
    override fun clear() {}
    override fun i(tag: String, event: String) {}
    override fun e(tag: String, event: String) {}
    override fun d(tag: String, event: String) {}
    override fun w(tag: String, event: String) {}
}

data class PlayerData(
    val incognitoMode: Boolean = false,
    val animeId: Long? = null,
    val animeTitle: String? = null,
    val episodeNumber: String? = null,
    val thumbnailUrl: String? = null,
)

// Enum class for standard Rich Presence in-app screens
enum class DiscordScreen(@StringRes val text: Int, @StringRes val details: Int, val imageUrl: String) {
    APP(R.string.app_name, R.string.browsing, animiruImageUrl),
    LIBRARY(R.string.label_library, R.string.browsing, libraryImageUrl),
    UPDATES(R.string.label_recent_updates, R.string.scrolling, updatesImageUrl),
    HISTORY(R.string.label_recent_manga, R.string.scrolling, historyImageUrl),
    BROWSE(R.string.browse, R.string.browsing, browseImageUrl),
    MORE(R.string.label_settings, R.string.messing, moreImageUrl),
    WEBVIEW(R.string.action_web_view, R.string.browsing, webviewImageUrl),
    VIDEO(R.string.video, R.string.watching, videoImageUrl),
    // Implement one for COMIC here, Luft
    ;
}

// Constants for standard Rich Presence image urls
// change the image Urls used here to match kuukiyomi brown/ green theme, Luft
private const val animiruImageUrl = "attachments/951705840031780865/1005845418405535784/Animiru.png"
private const val libraryImageUrl = "attachments/951705840031780865/1006843591777341520/library.png"
private const val updatesImageUrl = "attachments/951705840031780865/1006843592339365888/updates.png"
private const val historyImageUrl = "attachments/951705840031780865/1006843591299178588/history.png"
private const val browseImageUrl = "attachments/951705840031780865/1006843590980415518/browse.png"
private const val moreImageUrl = "attachments/951705840031780865/1006843592045760533/more.png"
private const val webviewImageUrl = "attachments/951705840031780865/1006843593467629568/webview.png"
private const val videoImageUrl = "attachments/951705840031780865/1006843592637169714/video.png"
// <-- AM (DISCORD)
