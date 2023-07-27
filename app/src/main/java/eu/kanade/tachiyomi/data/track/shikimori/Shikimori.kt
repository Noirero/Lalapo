package eu.kanade.tachiyomi.data.track.shikimori

import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.AnimeTrackService
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.injectLazy

class Shikimori(id: Long) : TrackService(id), AnimeTrackService {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 5
        const val REREADING = 6
    }

    private val json: Json by injectLazy()

    private val interceptor by lazy { ShikimoriInterceptor(this) }

    private val api by lazy { ShikimoriApi(client, interceptor) }

    @StringRes
    override fun nameRes() = R.string.tracker_shikimori

    override fun getScoreList(): List<String> {
        return IntRange(0, 10).map(Int::toString)
    }

    override fun indexToScore(index: Int): Float {
        return index.toFloat()
    }

    override fun displayScore(track: AnimeTrack): String {
        return track.score.toInt().toString()
    }

    private suspend fun add(track: AnimeTrack): AnimeTrack {
        return api.addLibAnime(track, getUsername())
    }

    override suspend fun update(track: AnimeTrack, didWatchEpisode: Boolean): AnimeTrack {
        if (track.status != COMPLETED) {
            if (didWatchEpisode) {
                if (track.last_episode_seen.toInt() == track.total_episodes && track.total_episodes > 0) {
                    track.status = COMPLETED
                } else if (track.status != REREADING) {
                    track.status = READING
                }
            }
        }

        return api.updateLibAnime(track, getUsername())
    }

    override suspend fun bind(track: AnimeTrack, hasSeenEpisodes: Boolean): AnimeTrack {
        val remoteTrack = api.findLibAnime(track, getUsername())
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id

            if (track.status != COMPLETED) {
                val isRereading = track.status == REREADING
                track.status = if (isRereading.not() && hasSeenEpisodes) READING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasSeenEpisodes) READING else PLAN_TO_READ
            track.score = 0F
            add(track)
        }
    }

    override suspend fun searchAnime(query: String): List<AnimeTrackSearch> {
        return api.searchAnime(query)
    }

    override suspend fun refresh(track: AnimeTrack): AnimeTrack {
        api.findLibAnime(track, getUsername())?.let { remoteTrack ->
            track.copyPersonalFrom(remoteTrack)
            track.total_episodes = remoteTrack.total_episodes
        }
        return track
    }

    override fun getLogo() = R.drawable.ic_tracker_shikimori

    override fun getLogoColor() = Color.rgb(40, 40, 40)

    override fun getStatusListAnime(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ, REREADING)
    }

    @StringRes
    override fun getStatus(status: Int): Int? = when (status) {
        READING -> R.string.reading
        PLAN_TO_READ -> R.string.plan_to_read
        COMPLETED -> R.string.completed
        ON_HOLD -> R.string.on_hold
        DROPPED -> R.string.dropped
        REREADING -> R.string.repeating
        else -> null
    }

    override fun getWatchingStatus(): Int = READING

    override fun getRewatchingStatus(): Int = REREADING

    override fun getCompletionStatus(): Int = COMPLETED

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(code: String) {
        try {
            val oauth = api.accessToken(code)
            interceptor.newAuth(oauth)
            val user = api.getCurrentUser()
            saveCredentials(user.toString(), oauth.access_token)
        } catch (e: Throwable) {
            logout()
        }
    }

    fun saveToken(oauth: OAuth?) {
        trackPreferences.trackToken(this).set(json.encodeToString(oauth))
    }

    fun restoreToken(): OAuth? {
        return try {
            json.decodeFromString<OAuth>(trackPreferences.trackToken(this).get())
        } catch (e: Exception) {
            null
        }
    }

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
        interceptor.newAuth(null)
    }
}
