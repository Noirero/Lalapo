package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.data.toLong
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.toSEpisode
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.toAnimeInfo
import eu.kanade.tachiyomi.data.database.models.toDomainAnime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import dataanime.Animes as DbAnime
import eu.kanade.domain.anime.interactor.GetFavorites as GetFavoritesAnime
import eu.kanade.domain.anime.model.Anime as DomainAnime

abstract class AbstractBackupManager(protected val context: Context) {

    protected val animehandler: AnimeDatabaseHandler = Injekt.get()
    internal val animesourceManager: AnimeSourceManager = Injekt.get()
    internal val trackManager: TrackManager = Injekt.get()
    protected val preferences: PreferencesHelper = Injekt.get()
    private val getFavoritesAnime: GetFavoritesAnime = Injekt.get()
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get()

    abstract suspend fun createBackup(uri: Uri, flags: Int, isAutoBackup: Boolean): String

    /**
     * Returns anime
     *
     * @return [Anime], null if not found
     */
    internal suspend fun getAnimeFromDatabase(url: String, source: Long): DbAnime? {
        return animehandler.awaitOneOrNull { animesQueries.getAnimeByUrlAndSource(url, source) }
    }

    /**
     * Fetches episode information.
     *
     * @param source source of anime
     * @param anime anime that needs updating
     * @param episodes list of episodes in the backup
     * @return Updated anime episodes.
     */
    internal suspend fun restoreEpisodes(source: AnimeSource, anime: Anime, episodes: List<Episode>): Pair<List<Episode>, List<Episode>> {
        val fetchedEpisodes = source.getEpisodeList(anime.toAnimeInfo())
            .map { it.toSEpisode() }
        val syncedEpisodes = syncEpisodesWithSource.await(fetchedEpisodes, anime.toDomainAnime()!!, source)
        if (syncedEpisodes.first.isNotEmpty()) {
            episodes.forEach { it.anime_id = anime.id }
            updateEpisodes(episodes)
        }
        return syncedEpisodes.first.map { it.toDbEpisode() } to syncedEpisodes.second.map { it.toDbEpisode() }
    }

    /**
     * Returns list containing anime from library
     *
     * @return [Anime] from library
     */
    protected suspend fun getFavoriteAnime(): List<DomainAnime> {
        return getFavoritesAnime.await()
    }

    /**
     * Inserts anime and returns id
     *
     * @return id of [Anime], null if not found
     */
    internal suspend fun insertAnime(anime: Anime): Long {
        return animehandler.awaitOne(true) {
            animesQueries.insert(
                source = anime.source,
                url = anime.url,
                artist = anime.artist,
                author = anime.author,
                description = anime.description,
                genre = anime.getGenres(),
                title = anime.title,
                status = anime.status.toLong(),
                thumbnailUrl = anime.thumbnail_url,
                favorite = anime.favorite,
                lastUpdate = anime.last_update,
                nextUpdate = 0L,
                initialized = anime.initialized,
                viewerFlags = anime.viewer_flags.toLong(),
                episodeFlags = anime.episode_flags.toLong(),
                coverLastModified = anime.cover_last_modified,
                dateAdded = anime.date_added,
            )
            animesQueries.selectLastInsertedRowId()
        }
    }

    internal suspend fun updateAnime(anime: Anime): Long {
        animehandler.await(true) {
            animesQueries.update(
                source = anime.source,
                url = anime.url,
                artist = anime.artist,
                author = anime.author,
                description = anime.description,
                genre = anime.genre,
                title = anime.title,
                status = anime.status.toLong(),
                thumbnailUrl = anime.thumbnail_url,
                favorite = anime.favorite.toLong(),
                lastUpdate = anime.last_update,
                initialized = anime.initialized.toLong(),
                viewer = anime.viewer_flags.toLong(),
                episodeFlags = anime.episode_flags.toLong(),
                coverLastModified = anime.cover_last_modified,
                dateAdded = anime.date_added,
                animeId = anime.id!!,
            )
        }
        return anime.id!!
    }

    /**
     * Inserts list of episodes
     */
    protected suspend fun insertEpisodes(episodes: List<Episode>) {
        animehandler.await(true) {
            episodes.forEach { episode ->
                episodesQueries.insert(
                    episode.anime_id!!,
                    episode.url,
                    episode.name,
                    episode.scanlator,
                    episode.seen,
                    episode.bookmark,
                    episode.fillermark,
                    episode.last_second_seen,
                    episode.total_seconds,
                    episode.episode_number,
                    episode.source_order.toLong(),
                    episode.date_fetch,
                    episode.date_upload,
                )
            }
        }
    }

    /**
     * Updates a list of episodes
     */
    protected suspend fun updateEpisodes(episodes: List<Episode>) {
        animehandler.await(true) {
            episodes.forEach { episode ->
                episodesQueries.update(
                    episode.anime_id!!,
                    episode.url,
                    episode.name,
                    episode.scanlator,
                    episode.seen.toLong(),
                    episode.bookmark.toLong(),
                    episode.fillermark.toLong(),
                    episode.last_second_seen,
                    episode.total_seconds,
                    episode.episode_number.toDouble(),
                    episode.source_order.toLong(),
                    episode.date_fetch,
                    episode.date_upload,
                    episode.id!!,
                )
            }
        }
    }

    /**
     * Updates a list of episodes with known database ids
     */
    protected suspend fun updateKnownEpisodes(episodes: List<Episode>) {
        animehandler.await(true) {
            episodes.forEach { episode ->
                episodesQueries.update(
                    animeId = null,
                    url = null,
                    name = null,
                    scanlator = null,
                    seen = episode.seen.toLong(),
                    bookmark = episode.bookmark.toLong(),
                    fillermark = episode.fillermark.toLong(),
                    lastSecondSeen = episode.last_second_seen,
                    totalSeconds = episode.total_seconds,
                    episodeNumber = null,
                    sourceOrder = null,
                    dateFetch = null,
                    dateUpload = null,
                    episodeId = episode.id!!,
                )
            }
        }
    }

    /**
     * Return number of backups.
     *
     * @return number of backups selected by user
     */
    protected fun numberOfBackups(): Int = preferences.numberOfBackups().get()
}
