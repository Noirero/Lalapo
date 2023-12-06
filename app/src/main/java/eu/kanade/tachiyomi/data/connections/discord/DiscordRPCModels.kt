// AM (DISCORD) -->

// Original library from https://github.com/dead8309/KizzyRPC (Thank you)
// Thank you to the 最高 man for the refactored and simplified code
// https://github.com/saikou-app/saikou
package eu.kanade.tachiyomi.data.connections.discord

import dev.icerock.moko.resources.StringResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import tachiyomi.i18n.MR

// Constant for logging tag
const val RICH_PRESENCE_TAG = "discord_rpc"

// Constant for application id
private const val RICH_PRESENCE_APPLICATION_ID = "952899285983326208"

// Constant for buttons list
private val RICH_PRESENCE_BUTTONS = listOf("Get the app!", "Join the Discord!")

// Constant for metadata list
private val RICH_PRESENCE_METADATA = Activity.Metadata(
    listOf(
        "https://github.com/Quickdesh/Animiru",
        "https://discord.gg/yDuHDMwxhv",
    ),
)

@Serializable
data class Activity(
    @SerialName("application_id")
    val applicationId: String? = RICH_PRESENCE_APPLICATION_ID,
    val name: String? = null,
    val details: String? = null,
    val state: String? = null,
    val type: Int? = null,
    val timestamps: Timestamps? = null,
    val assets: Assets? = null,
    val buttons: List<String>? = RICH_PRESENCE_BUTTONS,
    val metadata: Metadata? = RICH_PRESENCE_METADATA,
) {
    @Serializable
    data class Assets(
        @SerialName("large_image")
        val largeImage: String? = null,
        @SerialName("large_text")
        val largeText: String? = null,
        @SerialName("small_image")
        val smallImage: String? = null,
        @SerialName("small_text")
        val smallText: String? = null,
    )

    @Serializable
    data class Metadata(
        @SerialName("button_urls")
        val buttonUrls: List<String>,
    )

    @Serializable
    data class Timestamps(
        val start: Long? = null,
        val stop: Long? = null,
    )
}

@Serializable
data class Presence(
    val activities: List<Activity> = listOf(),
    val afk: Boolean = true,
    val since: Long? = null,
    val status: String? = null,
) {
    @Serializable
    data class Response(
        val op: Long,
        val d: Presence,
    )
}

@Serializable
data class Identity(
    val token: String,
    val properties: Properties,
    val compress: Boolean,
    val intents: Long,
) {

    @Serializable
    data class Response(
        val op: Long,
        val d: Identity,
    )

    @Serializable
    data class Properties(
        @SerialName("\$os")
        val os: String,

        @SerialName("\$browser")
        val browser: String,

        @SerialName("\$device")
        val device: String,
    )
}

@Serializable
data class Res(
    val t: String?,
    val s: Int?,
    val op: Int,
    val d: JsonElement,
)

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
}

data class PlayerData(
    val incognitoMode: Boolean = false,
    val animeId: Long? = null,
    val animeTitle: String? = null,
    val episodeNumber: String? = null,
    val thumbnailUrl: String? = null,
)

// Enum class for standard Rich Presence in-app screens
enum class DiscordScreen(val text: StringResource, val details: StringResource, val imageUrl: String) {
    APP(MR.strings.app_name, MR.strings.browsing, animiruImageUrl),
    LIBRARY(MR.strings.label_library, MR.strings.browsing, libraryImageUrl),
    UPDATES(MR.strings.label_recent_updates, MR.strings.scrolling, updatesImageUrl),
    HISTORY(MR.strings.label_recent_manga, MR.strings.scrolling, historyImageUrl),
    BROWSE(MR.strings.label_sources, MR.strings.browsing, browseImageUrl),
    MORE(MR.strings.label_settings, MR.strings.messing, moreImageUrl),
    WEBVIEW(MR.strings.action_web_view, MR.strings.browsing, webviewImageUrl),
    VIDEO(MR.strings.video, MR.strings.watching, videoImageUrl),
    // Implement one for COMIC here, Luft
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
