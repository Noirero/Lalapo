package eu.kanade.tachiyomi.data.track.bangumi

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class BangumiApi(
    private val trackId: Long,
    private val client: OkHttpClient,
    interceptor: BangumiInterceptor,
) {

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun addLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            val body = FormBody.Builder()
                .add("rating", track.score.toInt().toString())
                .add("status", track.toBangumiStatus())
                .build()
            authClient.newCall(POST("$apiUrl/collection/${track.remote_id}/update", body = body))
                .awaitSuccess()
            track
        }
    }

    suspend fun updateLibAnime(track: AnimeTrack): AnimeTrack {
        return withIOContext {
            // read status update
            val sbody = FormBody.Builder()
                .add("rating", track.score.toInt().toString())
                .add("status", track.toBangumiStatus())
                .build()
            authClient.newCall(POST("$apiUrl/collection/${track.remote_id}/update", body = sbody))
                .awaitSuccess()

            // chapter update
            val body = FormBody.Builder()
                .add("watched_eps", track.last_episode_seen.toInt().toString())
                .build()
            authClient.newCall(
                POST(
                    "$apiUrl/subject/${track.remote_id}/update/watched_eps",
                    body = body,
                ),
            ).awaitSuccess()

            track
        }
    }

    suspend fun searchAnime(search: String): List<AnimeTrackSearch> {
        return withIOContext {
            val url = "$apiUrl/search/subject/${URLEncoder.encode(
                search,
                StandardCharsets.UTF_8.name(),
            )}"
                .toUri()
                .buildUpon()
                .appendQueryParameter("max_results", "20")
                .build()
            authClient.newCall(GET(url.toString()))
                .awaitSuccess()
                .use {
                    var responseBody = it.body?.string().orEmpty()
                    if (responseBody.isEmpty()) {
                        throw Exception("Null Response")
                    }
                    if (responseBody.contains("\"code\":404")) {
                        responseBody = "{\"results\":0,\"list\":[]}"
                    }
                    val response = json.decodeFromString<JsonObject>(responseBody)["list"]?.jsonArray
                    response?.filter { it.jsonObject["type"]?.jsonPrimitive?.int == 1 }
                        ?.map { jsonToSearchAnime(it.jsonObject) }.orEmpty()
                }
        }
    }

    private fun jsonToSearchAnime(obj: JsonObject): AnimeTrackSearch {
        val coverUrl = if (obj["images"] is JsonObject) {
            obj["images"]?.jsonObject?.get("common")?.jsonPrimitive?.contentOrNull ?: ""
        } else {
            // Sometimes JsonNull
            ""
        }
        val totalChapters = if (obj["eps_count"] != null) {
            obj["eps_count"]!!.jsonPrimitive.long
        } else {
            0
        }

        val rating = obj["rating"]?.jsonObject?.get("score")?.jsonPrimitive?.doubleOrNull ?: -1.0
        return AnimeTrackSearch.create(trackId).apply {
            remote_id = obj["id"]!!.jsonPrimitive.long
            title = obj["name_cn"]!!.jsonPrimitive.content
            cover_url = coverUrl
            score = rating
            summary = obj["name"]!!.jsonPrimitive.content
            tracking_url = obj["url"]!!.jsonPrimitive.content
            total_episodes = totalChapters
        }
    }

    suspend fun findLibAnime(track: AnimeTrack): AnimeTrack? {
        return withIOContext {
            with(json) {
                authClient.newCall(GET("$apiUrl/subject/${track.remote_id}"))
                    .awaitSuccess()
                    .parseAs<JsonObject>()
                    .let { jsonToSearchAnime(it) }
            }
        }
    }

    suspend fun statusLibAnime(track: AnimeTrack): AnimeTrack? {
        return withIOContext {
            val urlUserRead = "$apiUrl/collection/${track.remote_id}"
            val requestUserRead = Request.Builder()
                .url(urlUserRead)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .build()

            // TODO: get user readed chapter here
            var response = authClient.newCall(requestUserRead).awaitSuccess()
            var responseBody = response.body?.string().orEmpty()
            if (responseBody.isEmpty()) {
                throw Exception("Null Response")
            }
            if (responseBody.contains("\"code\":400")) {
                null
            } else {
                json.decodeFromString<Collection>(responseBody).let {
                    track.status = it.status?.id!!
                    track.last_episode_seen = it.ep_status!!.toDouble()
                    track.score = it.rating!!
                    track
                }
            }
        }
    }

    suspend fun accessToken(code: String): OAuth {
        return withIOContext {
            with(json) {
                client.newCall(accessTokenRequest(code))
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    private fun accessTokenRequest(code: String) = POST(
        oauthUrl,
        body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .add("redirect_uri", redirectUrl)
            .build(),
    )

    companion object {
        private const val clientId = "bgm2204622cb426b1e78"
        private const val clientSecret = "5c9fd8953ebe3d10d6ea3f5e2d5f8508"

        private const val apiUrl = "https://api.bgm.tv"
        private const val oauthUrl = "https://bgm.tv/oauth/access_token"
        private const val loginUrl = "https://bgm.tv/oauth/authorize"

        private const val redirectUrl = "animiru://bangumi-auth"

        fun authUrl(): Uri =
            loginUrl.toUri().buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", redirectUrl)
                .build()

        fun refreshTokenRequest(token: String) = POST(
            oauthUrl,
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", token)
                .add("redirect_uri", redirectUrl)
                .build(),
        )
    }
}
