package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeHistory
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSerializer
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.database.models.anime.Anime
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.anime.Episode
import eu.kanade.tachiyomi.data.library.anime.CustomAnimeManager
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import kotlinx.coroutines.Job
import kotlinx.serialization.SerializationException
import okio.buffer
import okio.gzip
import okio.source
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import eu.kanade.tachiyomi.data.backup.full.models.BackupSerializer as FullBackupSerializer
import eu.kanade.tachiyomi.data.backup.full.models.BooleanPreferenceValue as FullBooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.FloatPreferenceValue as FullFloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.IntPreferenceValue as FullIntPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.LongPreferenceValue as FullLongPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.StringPreferenceValue as FullStringPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.StringSetPreferenceValue as FullStringSetPreferenceValue

class BackupRestorer(
    private val context: Context,
    private val notifier: BackupNotifier,
) {

    var job: Job? = null

    private var backupManager = BackupManager(context)

    private var restoreAmount = 0
    private var restoreProgress = 0

    /**
     * Mapping of source ID to source name from backup data
     */
    private var sourceMapping: Map<Long, String> = emptyMap()
    private var animeSourceMapping: Map<Long, String> = emptyMap()

    private val errors = mutableListOf<Pair<Date, String>>()

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

    fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("animiru_restore.txt")
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

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun performRestore(uri: Uri): Boolean {
        val backupString = context.contentResolver.openInputStream(uri)!!.source().gzip().buffer().use { it.readByteArray() }

        // Sadly, this is necessary because of old "full" backups.
        val backup = try {
            backupManager.parser.decodeFromByteArray(BackupSerializer, backupString)
        } catch (e: SerializationException) {
            val fullBackup = backupManager.parser.decodeFromByteArray(FullBackupSerializer, backupString)
            val backupPreferences = fullBackup.backupPreferences.map {
                val value = when (it.value) {
                    is FullIntPreferenceValue -> IntPreferenceValue(it.value.value)
                    is FullLongPreferenceValue -> LongPreferenceValue(it.value.value)
                    is FullFloatPreferenceValue -> FloatPreferenceValue(it.value.value)
                    is FullBooleanPreferenceValue -> BooleanPreferenceValue(it.value.value)
                    is FullStringPreferenceValue -> StringPreferenceValue(it.value.value)
                    is FullStringSetPreferenceValue -> StringSetPreferenceValue(it.value.value)
                }
                BackupPreference(it.key, value)
            }
            Backup(
                fullBackup.backupAnime,
                fullBackup.backupAnimeCategories,
                fullBackup.backupBrokenAnimeSources,
                fullBackup.backupAnimeSources,
                backupPreferences,
            )
        }

        restoreAmount = backup.backupAnime.size + 2 // +2 for categories

        // Restore categories

        if (backup.backupAnimeCategories.isNotEmpty()) {
            restoreAnimeCategories(backup.backupAnimeCategories)
        }

        // Store source mapping for error messages

        val backupAnimeMaps = backup.backupBrokenAnimeSources.map { BackupAnimeSource(it.name, it.sourceId) } + backup.backupAnimeSources
        animeSourceMapping = backupAnimeMaps.associate { it.sourceId to it.name }

        // Restore individual manga

        backup.backupAnime.forEach {
            if (job?.isActive != true) {
                return false
            }

            restoreAnime(it, backup.backupAnimeCategories)
        }

        // TODO: optionally trigger online library + tracker update

        if (backup.backupPreferences.isNotEmpty()) {
            restorePreferences(backup.backupPreferences)
        }

        return true
    }

    private suspend fun restoreAnimeCategories(backupCategories: List<BackupCategory>) {
        backupManager.restoreAnimeCategories(backupCategories)

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.anime_categories))
    }

    private suspend fun restoreAnime(backupAnime: BackupAnime, backupCategories: List<BackupCategory>) {
        val anime = backupAnime.getAnimeImpl()
        val episodes = backupAnime.getEpisodesImpl()
        val categories = backupAnime.categories.map { it.toInt() }
        val history =
            backupAnime.brokenHistory.map { BackupAnimeHistory(it.url, it.lastSeen) } + backupAnime.history
        val tracks = backupAnime.getTrackingImpl()
        // AM (CU) -->
        val customAnime = backupAnime.getCustomAnimeInfo()
        // <-- AM (CU)

        try {
            val dbAnime = backupManager.getAnimeFromDatabase(anime.url, anime.source)
            if (dbAnime == null) {
                // Anime not in database
                // AM (CU)>
                restoreExistingAnime(anime, episodes, categories, history, tracks, backupCategories, customAnime)
            } else {
                // Anime in database
                // Copy information from anime already in database
                backupManager.restoreExistingAnime(anime, dbAnime)
                // Fetch rest of anime information
                // AM (CU)>
                restoreNewAnime(anime, episodes, categories, history, tracks, backupCategories, customAnime)
            }
        } catch (e: Exception) {
            val sourceName = sourceMapping[anime.source] ?: anime.source.toString()
            errors.add(Date() to "${anime.title} [$sourceName]: ${e.message}")
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, anime.title)
    }

    /**
     * Fetches anime information
     *
     * @param anime anime that needs updating
     * @param episodes episodes of anime that needs updating
     * @param categories categories that need updating
     */
    private suspend fun restoreExistingAnime(
        anime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>,
        // AM (CU) -->
        customAnime: CustomAnimeManager.AnimeJson?,
        // <-- AM (CU)
    ) {
        val fetchedAnime = backupManager.restoreNewAnime(anime)
        fetchedAnime.id ?: return

        backupManager.restoreEpisodes(fetchedAnime, episodes)
        // AM (CU)>
        restoreExtras(fetchedAnime, categories, history, tracks, backupCategories, customAnime)
    }

    private suspend fun restoreNewAnime(
        backupAnime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>,
        // AM (CU) -->
        customAnime: CustomAnimeManager.AnimeJson?,
        // <-- AM (CU)
    ) {
        backupManager.restoreEpisodes(backupAnime, episodes)
        // AM (CU)>
        restoreExtras(backupAnime, categories, history, tracks, backupCategories, customAnime)
    }

    private suspend fun restoreExtras(
        anime: Anime,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>,
        // AM (CU) -->
        customAnime: CustomAnimeManager.AnimeJson?,
        // <-- AM (CU)
    ) {
        backupManager.restoreAnimeCategories(anime, categories, backupCategories)
        backupManager.restoreAnimeHistory(history)
        backupManager.restoreAnimeTracking(anime, tracks)
        // AM (CU) -->
        customAnime?.id = anime.id!!
        backupManager.restoreEditedInfo(customAnime)
        // <-- AM (CU)
    }

    private fun restorePreferences(preferences: List<BackupPreference>) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.forEach { pref ->
            when (pref.value) {
                is IntPreferenceValue -> {
                    if (prefs.all[pref.key] is Int?) {
                        prefs.edit().putInt(pref.key, pref.value.value).apply()
                    }
                }
                is LongPreferenceValue -> {
                    if (prefs.all[pref.key] is Long?) {
                        prefs.edit().putLong(pref.key, pref.value.value).apply()
                    }
                }
                is FloatPreferenceValue -> {
                    if (prefs.all[pref.key] is Float?) {
                        prefs.edit().putFloat(pref.key, pref.value.value).apply()
                    }
                }
                is StringPreferenceValue -> {
                    if (prefs.all[pref.key] is String?) {
                        prefs.edit().putString(pref.key, pref.value.value).apply()
                    }
                }
                is BooleanPreferenceValue -> {
                    if (prefs.all[pref.key] is Boolean?) {
                        prefs.edit().putBoolean(pref.key, pref.value.value).apply()
                    }
                }
                is StringSetPreferenceValue -> {
                    if (prefs.all[pref.key] is Set<*>?) {
                        prefs.edit().putStringSet(pref.key, pref.value.value).apply()
                    }
                }
            }
        }
    }

    /**
     * Called to update dialog in [BackupConst]
     *
     * @param progress restore progress
     * @param amount total restoreAmount of anime and manga
     * @param title title of restored anime and manga
     */
    private fun showRestoreProgress(progress: Int, amount: Int, title: String) {
        notifier.showRestoreProgress(title, progress, amount)
    }
}
