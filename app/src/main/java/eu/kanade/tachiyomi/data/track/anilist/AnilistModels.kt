package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.util.lang.htmlDecode
import kotlinx.serialization.Serializable
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import tachiyomi.domain.track.anime.model.AnimeTrack as DomainAnimeTrack

data class ALAnime(
    val remote_id: Long,
    val title_user_pref: String,
    val image_url_lge: String,
    val description: String?,
    val format: String,
    val publishing_status: String,
    val start_date_fuzzy: Long,
    val total_episodes: Long,
    val average_score: Int,
) {

    fun toTrack() = AnimeTrackSearch.create(TrackerManager.ANILIST).apply {
        remote_id = this@ALAnime.remote_id
        title = title_user_pref
        total_episodes = this@ALAnime.total_episodes
        cover_url = image_url_lge
        summary = description?.htmlDecode() ?: ""
        score = average_score.toDouble()
        tracking_url = AnilistApi.animeUrl(remote_id)
        publishing_status = this@ALAnime.publishing_status
        publishing_type = format
        if (start_date_fuzzy != 0L) {
            start_date = try {
                val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                outputDf.format(start_date_fuzzy)
            } catch (e: Exception) {
                ""
            }
        }
    }
}

data class ALUserAnime(
    val library_id: Long,
    val list_status: String,
    val score_raw: Int,
    val episodes_seen: Int,
    val start_date_fuzzy: Long,
    val completed_date_fuzzy: Long,
    val anime: ALAnime,
) {

    fun toTrack() = AnimeTrack.create(TrackerManager.ANILIST).apply {
        remote_id = anime.remote_id
        title = anime.title_user_pref
        status = toTrackStatus()
        score = score_raw.toDouble()
        started_watching_date = start_date_fuzzy
        finished_watching_date = completed_date_fuzzy
        last_episode_seen = episodes_seen.toDouble()
        library_id = this@ALUserAnime.library_id
        total_episodes = anime.total_episodes
    }

    private fun toTrackStatus() = when (list_status) {
        "CURRENT" -> Anilist.WATCHING
        "COMPLETED" -> Anilist.COMPLETED
        "PAUSED" -> Anilist.PAUSED
        "DROPPED" -> Anilist.DROPPED
        "PLANNING" -> Anilist.PLANNING_ANIME
        "REPEATING" -> Anilist.REPEATING_ANIME
        else -> throw NotImplementedError("Unknown status: $list_status")
    }
}

@Serializable
data class OAuth(
    val access_token: String,
    val token_type: String,
    val expires: Long,
    val expires_in: Long,
)

fun OAuth.isExpired() = System.currentTimeMillis() > expires

fun AnimeTrack.toAnilistStatus() = when (status) {
    Anilist.WATCHING -> "CURRENT"
    Anilist.COMPLETED -> "COMPLETED"
    Anilist.PAUSED -> "PAUSED"
    Anilist.DROPPED -> "DROPPED"
    Anilist.PLANNING_ANIME -> "PLANNING"
    Anilist.REPEATING_ANIME -> "REPEATING"
    else -> throw NotImplementedError("Unknown status: $status")
}

private val preferences: TrackPreferences by injectLazy()

fun DomainAnimeTrack.toAnilistScore(): String = when (preferences.anilistScoreType().get()) {
    // 10 point
    "POINT_10" -> (score.toInt() / 10).toString()
    // 100 point
    "POINT_100" -> score.toInt().toString()
    // 5 stars
    "POINT_5" -> when {
        score == 0.0 -> "0"
        score < 30 -> "1"
        score < 50 -> "2"
        score < 70 -> "3"
        score < 90 -> "4"
        else -> "5"
    }
    // Smiley
    "POINT_3" -> when {
        score == 0.0 -> "0"
        score <= 35 -> ":("
        score <= 60 -> ":|"
        else -> ":)"
    }
    // 10 point decimal
    "POINT_10_DECIMAL" -> (score / 10).toString()
    else -> throw NotImplementedError("Unknown score type")
}
