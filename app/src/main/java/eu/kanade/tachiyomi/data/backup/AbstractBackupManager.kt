package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.toSEpisode
import eu.kanade.tachiyomi.data.animelib.CustomAnimeManager
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.toAnimeInfo
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.episode.syncEpisodesWithSource
import uy.kohesive.injekt.injectLazy

abstract class AbstractBackupManager(protected val context: Context) {

    internal val animedatabaseHelper: AnimeDatabaseHelper by injectLazy()
    internal val animesourceManager: AnimeSourceManager by injectLazy()
    internal val trackManager: TrackManager by injectLazy()
    protected val preferences: PreferencesHelper by injectLazy()
    protected val customAnimeManager: CustomAnimeManager by injectLazy()

    abstract fun createBackup(uri: Uri, flags: Int, isJob: Boolean): String?

    /**
     * Returns manga
     *
     * @return [Manga], null if not found
     */
    internal fun getAnimeFromDatabase(anime: Anime): Anime? =
        animedatabaseHelper.getAnime(anime.url, anime.source).executeAsBlocking()

    /**
     * Fetches chapter information.
     *
     * @param source source of manga
     * @param anime anime that needs updating
     * @param episodes list of episodes in the backup
     * @return Updated manga chapters.
     */
    internal suspend fun restoreEpisodes(source: AnimeSource, anime: Anime, episodes: List<Episode>): Pair<List<Episode>, List<Episode>> {
        val fetchedEpisodes = source.getEpisodeList(anime.toAnimeInfo())
            .map { it.toSEpisode() }
        val syncedEpisodes = syncEpisodesWithSource(animedatabaseHelper, fetchedEpisodes, anime, source)
        if (syncedEpisodes.first.isNotEmpty()) {
            episodes.forEach { it.anime_id = anime.id }
            updateEpisodes(episodes)
        }
        return syncedEpisodes
    }

    /**
     * Returns list containing anime from library
     *
     * @return [Anime] from library
     */
    protected fun getFavoriteAnime(): List<Anime> =
        animedatabaseHelper.getFavoriteAnimes().executeAsBlocking()

    protected fun getSeenAnime(): List<Anime> =
        animedatabaseHelper.getSeenNotInLibraryAnimes().executeAsBlocking()

    /**
     * Inserts anime and returns id
     *
     * @return id of [Anime], null if not found
     */
    internal fun insertAnime(anime: Anime): Long? =
        animedatabaseHelper.insertAnime(anime).executeAsBlocking().insertedId()

    /**
     * Inserts list of chapters
     */
    protected fun insertEpisodes(episodes: List<Episode>) {
        animedatabaseHelper.insertEpisodes(episodes).executeAsBlocking()
    }

    /**
     * Updates a list of chapters
     */
    protected fun updateEpisodes(episodes: List<Episode>) {
        animedatabaseHelper.updateEpisodesBackup(episodes).executeAsBlocking()
    }

    /**
     * Updates a list of chapters with known database ids
     */
    protected fun updateKnownEpisodes(episodes: List<Episode>) {
        animedatabaseHelper.updateKnownEpisodesBackup(episodes).executeAsBlocking()
    }

    /**
     * Return number of backups.
     *
     * @return number of backups selected by user
     */
    protected fun numberOfBackups(): Int = preferences.numberOfBackups().get()
}
