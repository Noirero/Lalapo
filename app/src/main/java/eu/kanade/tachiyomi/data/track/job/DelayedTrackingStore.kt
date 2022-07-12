package eu.kanade.tachiyomi.data.track.job

import android.content.Context
import androidx.core.content.edit
import eu.kanade.domain.animetrack.model.AnimeTrack
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class DelayedTrackingStore(context: Context) {

    /**
     * Preference file where queued tracking updates are stored.
     */
    private val animePreferences = context.getSharedPreferences("tracking_queue_anime", Context.MODE_PRIVATE)

    fun addItem(track: AnimeTrack) {
        val trackId = track.id.toString()
        val (_, lastEpisodeSeen) = animePreferences.getString(trackId, "0:0.0")!!.split(":")
        if (track.lastEpisodeSeen > lastEpisodeSeen.toFloat()) {
            val value = "${track.animeId}:${track.lastEpisodeSeen}"
            logcat(LogPriority.DEBUG) { ("Queuing track item: $trackId, $value") }
            animePreferences.edit {
                putString(trackId, value)
            }
        }
    }

    fun clear() {
        animePreferences.edit {
            clear()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getAnimeItems(): List<DelayedAnimeTrackingItem> {
        return (animePreferences.all as Map<String, String>).entries
            .map {
                val (animeId, lastEpisodeSeen) = it.value.split(":")
                DelayedAnimeTrackingItem(
                    trackId = it.key.toLong(),
                    animeId = animeId.toLong(),
                    lastEpisodeSeen = lastEpisodeSeen.toFloat(),
                )
            }
    }

    data class DelayedAnimeTrackingItem(
        val trackId: Long,
        val animeId: Long,
        val lastEpisodeSeen: Float,
    )
}
