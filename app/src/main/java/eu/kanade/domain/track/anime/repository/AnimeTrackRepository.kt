package eu.kanade.domain.track.anime.repository

import eu.kanade.domain.track.anime.model.AnimeTrack
import kotlinx.coroutines.flow.Flow

interface AnimeTrackRepository {

    // AM (GU) -->
    suspend fun getTracks(): List<AnimeTrack>
    // <-- AM (GU)

    suspend fun getTrackByAnimeId(id: Long): AnimeTrack?

    suspend fun getTracksByAnimeId(animeId: Long): List<AnimeTrack>

    fun getAnimeTracksAsFlow(): Flow<List<AnimeTrack>>

    fun getTracksByAnimeIdAsFlow(animeId: Long): Flow<List<AnimeTrack>>

    suspend fun deleteAnime(animeId: Long, syncId: Long)

    suspend fun insertAnime(track: AnimeTrack)

    suspend fun insertAllAnime(tracks: List<AnimeTrack>)
}
