package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.data.DatabaseHandler
import eu.kanade.data.chapter.NoChaptersException
import eu.kanade.data.episode.NoEpisodesException
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.animelib.CustomAnimeManager
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import kotlinx.coroutines.Job
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

abstract class AbstractBackupRestore<T : AbstractBackupManager>(protected val context: Context, protected val notifier: BackupNotifier) {

    protected val animehandler: AnimeDatabaseHandler by injectLazy()
    protected val trackManager: TrackManager by injectLazy()

    protected val customAnimeManager: CustomAnimeManager by injectLazy()

    var job: Job? = null

    protected lateinit var backupManager: T

    protected var restoreAmount = 0
    protected var restoreProgress = 0

    /**
     * Mapping of source ID to source name from backup data
     */
    protected var sourceMapping: Map<Long, String> = emptyMap()

    protected val errors = mutableListOf<Pair<Date, String>>()

    abstract suspend fun performRestore(uri: Uri): Boolean

    suspend fun restoreBackup(uri: Uri): Boolean {
        val startTime = System.currentTimeMillis()
        restoreProgress = 0
        errors.clear()

        if (!performRestore(uri)) {
            return false
        }

        val endTime = System.currentTimeMillis()
        val time = endTime - startTime

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(time, errors.size, logFile.parent, logFile.name)
        return true
    }

    /**
     * Fetches episode information.
     *
     * @param source source of manga
     * @param anime manga that needs updating
     * @return Updated manga chapters.
     */
    internal suspend fun updateEpisodes(source: AnimeSource, anime: Anime, episodes: List<Episode>): Pair<List<Episode>, List<Episode>> {
        return try {
            backupManager.restoreEpisodes(source, anime, episodes)
        } catch (e: Exception) {
            // If there's any error, return empty update and continue.
            val errorMessage = if (e is NoEpisodesException) {
                context.getString(R.string.no_episodes_error)
            } else {
                e.message
            }
            errors.add(Date() to "${anime.title} - $errorMessage")
            Pair(emptyList(), emptyList())
        }
    }

    /**
     * Refreshes tracking information.
     *
     * @param anime that needs updating.
     * @param tracks list containing tracks from restore file.
     */
    internal suspend fun updateAnimeTracking(anime: Anime, tracks: List<AnimeTrack>) {
        tracks.forEach { track ->
            val service = trackManager.getService(track.sync_id.toLong())
            if (service != null && service.isLogged) {
                try {
                    val updatedTrack = service.refresh(track)
                    animehandler.await {
                        anime_syncQueries.insert(
                            updatedTrack.anime_id,
                            updatedTrack.sync_id.toLong(),
                            updatedTrack.media_id,
                            updatedTrack.library_id,
                            updatedTrack.title,
                            updatedTrack.last_episode_seen.toDouble(),
                            updatedTrack.total_episodes.toLong(),
                            updatedTrack.status.toLong(),
                            updatedTrack.score,
                            updatedTrack.tracking_url,
                            updatedTrack.started_watching_date,
                            updatedTrack.finished_watching_date,
                        )
                    }
                } catch (e: Exception) {
                    errors.add(Date() to "${anime.title} - ${e.message}")
                }
            } else {
                val serviceName = service?.nameRes()?.let { context.getString(it) }
                errors.add(Date() to "${anime.title} - ${context.getString(R.string.tracker_not_logged_in, serviceName)}")
            }
        }
    }

    /**
     * Called to update dialog in [BackupConst]
     *
     * @param progress restore progress
     * @param amount total restoreAmount of manga
     * @param title title of restored manga
     */
    internal fun showRestoreProgress(
        progress: Int,
        amount: Int,
        title: String,
    ) {
        notifier.showRestoreProgress(title, progress, amount)
    }

    internal fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("Animiru_restore.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                file.bufferedWriter().use { out ->
                    errors.forEach { (date, message) ->
                        out.write("[${sdf.format(date)}] $message\n")
                    }
                }
                return file
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }
}
