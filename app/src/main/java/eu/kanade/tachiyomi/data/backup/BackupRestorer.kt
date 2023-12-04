package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import data.Manga_sync
import data.Mangas
import dataanime.Anime_sync
import dataanime.Animes
import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeHistory
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupExtension
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionPreferences
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import eu.kanade.tachiyomi.source.anime.model.copyFrom
import eu.kanade.tachiyomi.source.manga.model.copyFrom
import eu.kanade.tachiyomi.util.BackupUtil
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import tachiyomi.core.i18n.stringResource
import tachiyomi.core.util.system.logcat
import tachiyomi.data.AnimeUpdateStrategyColumnAdapter
import tachiyomi.data.MangaUpdateStrategyColumnAdapter
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.manga.interactor.GetMangaCategories
import tachiyomi.domain.entries.anime.interactor.AnimeFetchInterval
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.history.anime.model.AnimeHistoryUpdate
import tachiyomi.domain.items.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.entries.anime.model.CustomAnimeInfo
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.domain.track.manga.model.MangaTrack
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import kotlin.math.max

class BackupRestorer(
    private val context: Context,
    private val notifier: BackupNotifier,
) {

    private val mangaHandler: MangaDatabaseHandler = Injekt.get()
    private val updateManga: UpdateManga = Injekt.get()
    private val getMangaCategories: GetMangaCategories = Injekt.get()
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get()
    private val mangaFetchInterval: MangaFetchInterval = Injekt.get()

    private val animeHandler: AnimeDatabaseHandler = Injekt.get()
    private val updateAnime: UpdateAnime = Injekt.get()
    private val getAnimeCategories: GetAnimeCategories = Injekt.get()
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId = Injekt.get()
    private val animeFetchInterval: AnimeFetchInterval = Injekt.get()

    private val libraryPreferences: LibraryPreferences = Injekt.get()

    private var now = ZonedDateTime.now()
    private var currentMangaFetchWindow = mangaFetchInterval.getWindow(now)
    private var currentAnimeFetchWindow = animeFetchInterval.getWindow(now)

    private var restoreAmount = 0
    private var restoreProgress = 0

    /**
     * Mapping of source ID to source name from backup data
     */
    private var sourceMapping: Map<Long, String> = emptyMap()
    private var animeSourceMapping: Map<Long, String> = emptyMap()

    private val errors = mutableListOf<Pair<Date, String>>()

    suspend fun syncFromBackup(uri: Uri, sync: Boolean): Boolean {
        val startTime = System.currentTimeMillis()
        restoreProgress = 0
        errors.clear()

        if (!performRestore(uri, sync)) {
            return false
        }

        val endTime = System.currentTimeMillis()
        val time = endTime - startTime

        val logFile = writeErrorLog()

        if (sync) {
            notifier.showRestoreComplete(
                time,
                errors.size,
                logFile.parent,
                logFile.name,
                contentTitle = context.stringResource(MR.strings.library_sync_complete),
            )
        } else {
            notifier.showRestoreComplete(time, errors.size, logFile.parent, logFile.name)
        }
        return true
    }

    private fun writeErrorLog(): File {
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
    private suspend fun performRestore(uri: Uri, sync: Boolean): Boolean {
        val backup = BackupUtil.decodeBackup(context, uri)

        restoreAmount =
            backup.backupAnime.size + 3 // +3 for categories, app prefs, source prefs

        // Restore categories

        if (backup.backupAnimeCategories.isNotEmpty()) {
            restoreAnimeCategories(backup.backupAnimeCategories)
        }

        // Store source mapping for error messages

        val backupAnimeMaps = backup.backupBrokenAnimeSources.map {
            BackupAnimeSource(
                it.name,
                it.sourceId,
            )
        } + backup.backupAnimeSources
        animeSourceMapping = backupAnimeMaps.associate { it.sourceId to it.name }

        now = ZonedDateTime.now()
        currentMangaFetchWindow = mangaFetchInterval.getWindow(now)
        currentAnimeFetchWindow = animeFetchInterval.getWindow(now)

        return coroutineScope {
            
            // Restore individual anime
            backup.backupAnime.forEach {
                if (!isActive) {
                    return@coroutineScope false
                }

                restoreAnime(it, backup.backupAnimeCategories, sync)
            }

            if (backup.backupPreferences.isNotEmpty()) {
                restorePreferences(
                    backup.backupPreferences,
                    PreferenceManager.getDefaultSharedPreferences(context),
                )
            }

            if (backup.backupExtensionPreferences.isNotEmpty()) {
                restoreExtensionPreferences(backup.backupExtensionPreferences)
            }

            if (backup.backupExtensions.isNotEmpty()) {
                restoreExtensions(backup.backupExtensions)
            }

            // TODO: optionally trigger online library + tracker update
            true
        }
    }

    private suspend fun restoreAnimeCategories(backupCategories: List<BackupCategory>) {
        // Get categories from file and from db
        val dbCategories = getAnimeCategories.await()

        val categories = backupCategories.map {
            var category = it.getCategory()
            var found = false
            for (dbCategory in dbCategories) {
                // If the category is already in the db, assign the id to the file's category
                // and do nothing
                if (category.name == dbCategory.name) {
                    category = category.copy(id = dbCategory.id)
                    found = true
                    break
                }
            }
            if (!found) {
                // Let the db assign the id
                val id = animeHandler.awaitOneExecutable {
                    categoriesQueries.insert(category.name, category.order, category.flags)
                    categoriesQueries.selectLastInsertedRowId()
                }
                category = category.copy(id = id)
            }

            category
        }

        libraryPreferences.categorizedDisplaySettings().set(
            (dbCategories + categories)
                .distinctBy { it.flags }
                .size > 1,
        )

        restoreProgress += 1
        showRestoreProgress(
            restoreProgress,
            restoreAmount,
            context.stringResource(MR.strings.anime_categories),
            context.stringResource(MR.strings.restoring_backup),
        )
    }

    private suspend fun restoreAnime(
        backupAnime: BackupAnime,
        backupCategories: List<BackupCategory>,
        sync: Boolean,
    ) {
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
            val dbAnime = getAnimeFromDatabase(anime.url, anime.source)
            val restoredAnime = if (dbAnime == null) {
                // Anime not in database
                // AM (CU)>
                restoreExistingAnime(anime, episodes, categories, history, tracks, backupCategories, customAnime)
            } else {
                // Anime in database
                // Copy information from anime already in database
                val updateAnime = restoreExistingAnime(anime, dbAnime)
                // Fetch rest of anime information
                // AM (CU)>
                restoreNewAnime(
                    updateAnime,
                    episodes,
                    categories,
                    history,
                    tracks,
                    backupCategories,
                    customAnime,
                )
            }
            updateAnime.awaitUpdateFetchInterval(restoredAnime, now, currentAnimeFetchWindow)
        } catch (e: Exception) {
            val sourceName = sourceMapping[anime.source] ?: anime.source.toString()
            errors.add(Date() to "${anime.title} [$sourceName]: ${e.message}")
        }

        restoreProgress += 1
        if (sync) {
            showRestoreProgress(
                restoreProgress,
                restoreAmount,
                anime.title,
                context.stringResource(MR.strings.syncing_library),
            )
        } else {
            showRestoreProgress(
                restoreProgress,
                restoreAmount,
                anime.title,
                context.stringResource(MR.strings.restoring_backup),
            )
        }
    }

    /**
     * Returns anime
     *
     * @return [Anime], null if not found
     */
    private suspend fun getAnimeFromDatabase(url: String, source: Long): Animes? {
        return animeHandler.awaitOneOrNull { animesQueries.getAnimeByUrlAndSource(url, source) }
    }

    private suspend fun restoreExistingAnime(anime: Anime, dbAnime: Animes): Anime {
        var updatedAnime = anime.copy(id = dbAnime._id)
        updatedAnime = updatedAnime.copyFrom(dbAnime)
        updateAnime(updatedAnime)
        return updatedAnime
    }

    private suspend fun updateAnime(anime: Anime): Long {
        animeHandler.await(true) {
            animesQueries.update(
                source = anime.source,
                url = anime.url,
                artist = anime.artist,
                author = anime.author,
                description = anime.description,
                genre = anime.genre?.joinToString(separator = ", "),
                title = anime.title,
                status = anime.status,
                thumbnailUrl = anime.thumbnailUrl,
                favorite = anime.favorite,
                lastUpdate = anime.lastUpdate,
                nextUpdate = null,
                calculateInterval = null,
                initialized = anime.initialized,
                viewer = anime.viewerFlags,
                episodeFlags = anime.episodeFlags,
                coverLastModified = anime.coverLastModified,
                dateAdded = anime.dateAdded,
                animeId = anime.id!!,
                updateStrategy = anime.updateStrategy.let(AnimeUpdateStrategyColumnAdapter::encode),
            )
        }
        return anime.id
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
        customAnime: CustomAnimeInfo?,
        // <-- AM (CU)
    ): Anime {
        val fetchedAnime = restoreNewAnime(anime)
        restoreEpisodes(fetchedAnime, episodes)
        // AM (CU)>
        restoreExtras(fetchedAnime, categories, history, tracks, backupCategories, customAnime)
        return fetchedAnime
    }

    private suspend fun restoreEpisodes(
        anime: Anime,
        episodes: List<Episode>,
    ) {
        val dbEpisodesByUrl = getEpisodesByAnimeId.await(anime.id)
            .associateBy { it.url }

        val processed = episodes.map { episode ->
            var updatedEpisode = episode

            val dbEpisode = dbEpisodesByUrl[updatedEpisode.url]
            if (dbEpisode != null) {
                updatedEpisode = updatedEpisode
                    .copyFrom(dbEpisode)
                    .copy(
                        id = dbEpisode.id,
                        bookmark = updatedEpisode.bookmark || dbEpisode.bookmark,
                        // AM (FILLER) -->
                        fillermark = updatedEpisode.fillermark || dbEpisode.fillermark,
                        // <-- AM (FILLER)
                    )
                if (dbEpisode.seen && !updatedEpisode.seen) {
                    updatedEpisode = updatedEpisode.copy(
                        seen = true,
                        lastSecondSeen = dbEpisode.lastSecondSeen,
                    )
                } else if (updatedEpisode.lastSecondSeen == 0L && dbEpisode.lastSecondSeen != 0L) {
                    updatedEpisode = updatedEpisode.copy(
                        lastSecondSeen = dbEpisode.lastSecondSeen,
                    )
                }
            }

            updatedEpisode.copy(animeId = anime.id)
        }

        val (existingEpisodes, newEpisodes) = processed.partition { it.id > 0 }
        updateKnownEpisodes(existingEpisodes)
        insertEpisodes(newEpisodes)
    }

    /**
     * Inserts list of episodes
     */
    private suspend fun insertEpisodes(episodes: List<Episode>) {
        animeHandler.await(true) {
            episodes.forEach { episode ->
                episodesQueries.insert(
                    episode.animeId,
                    episode.url,
                    episode.name,
                    episode.scanlator,
                    episode.seen,
                    episode.bookmark,
                    // AM (FILLER) -->
                    episode.filelrmark,
                    // <-- AM (FILLER)
                    episode.lastSecondSeen,
                    episode.totalSeconds,
                    episode.episodeNumber,
                    episode.sourceOrder,
                    episode.dateFetch,
                    episode.dateUpload,
                )
            }
        }
    }

    /**
     * Updates a list of episodes with known database ids
     */
    private suspend fun updateKnownEpisodes(
        episodes: List<Episode>,
    ) {
        animeHandler.await(true) {
            episodes.forEach { episode ->
                episodesQueries.update(
                    animeId = null,
                    url = null,
                    name = null,
                    scanlator = null,
                    seen = episode.seen,
                    bookmark = episode.bookmark,
                    // AM (FILLER) -->
                    fillermark = episode.fillermark,
                    // <-- AM (FILLER)
                    lastSecondSeen = episode.lastSecondSeen,
                    totalSeconds = episode.totalSeconds,
                    episodeNumber = null,
                    sourceOrder = null,
                    dateFetch = null,
                    dateUpload = null,
                    episodeId = episode.id,
                )
            }
        }
    }

    /**
     * Fetches anime information
     *
     * @param anime anime that needs updating
     * @return Updated anime info.
     */
    private suspend fun restoreNewAnime(anime: Anime): Anime {
        return anime.copy(
            initialized = anime.description != null,
            id = insertAnime(anime),
        )
    }

    /**
     * Inserts anime and returns id
     *
     * @return id of [Anime], null if not found
     */
    private suspend fun insertAnime(anime: Anime): Long {
        return animeHandler.awaitOneExecutable(true) {
            animesQueries.insert(
                source = anime.source,
                url = anime.url,
                artist = anime.artist,
                author = anime.author,
                description = anime.description,
                genre = anime.genre,
                title = anime.title,
                status = anime.status.toLong(),
                thumbnailUrl = anime.thumbnailUrl,
                favorite = anime.favorite,
                lastUpdate = anime.lastUpdate,
                nextUpdate = 0L,
                calculateInterval = 0L,
                initialized = anime.initialized,
                viewerFlags = anime.viewerFlags,
                episodeFlags = anime.episodeFlags,
                coverLastModified = anime.coverLastModified,
                dateAdded = anime.dateAdded,
                updateStrategy = anime.updateStrategy,
            )
            animesQueries.selectLastInsertedRowId()
        }
    }

    private suspend fun restoreNewAnime(
        backupAnime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>,
        // AM (CU) -->
        customAnime: CustomAnimeInfo?,
        // <-- AM (CU)
    ): Anime {
        restoreEpisodes(backupAnime, episodes)
        // AM (CU)>
        restoreExtras(backupAnime, categories, history, tracks, backupCategories, customAnime)
        return backupAnime
    }

    private suspend fun restoreExtras(
        anime: Anime,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>,
        // AM (CU) -->
        customAnime: CustomAnimeInfo?,
        // <-- AM (CU)
    ) {
        restoreAnimeCategories(anime, categories, backupCategories)
        restoreAnimeHistory(history)
        restoreAnimeTracking(anime, tracks)
        // AM (CU) -->
        restoreEditedInfo(customAnime?.copy(id = anime.id))
        // <-- AM (CU)
    }

    /**
     * Restores the categories an anime is in.
     *
     * @param anime the anime whose categories have to be restored.
     * @param categories the categories to restore.
     */
    private suspend fun restoreAnimeCategories(
        anime: Anime,
        categories: List<Int>,
        backupCategories: List<BackupCategory>,
    ) {
        val dbCategories = getAnimeCategories.await()
        val animeCategoriesToUpdate = mutableListOf<Pair<Long, Long>>()

        categories.forEach { backupCategoryOrder ->
            backupCategories.firstOrNull {
                it.order == backupCategoryOrder.toLong()
            }?.let { backupCategory ->
                dbCategories.firstOrNull { dbCategory ->
                    dbCategory.name == backupCategory.name
                }?.let { dbCategory ->
                    animeCategoriesToUpdate.add(Pair(anime.id, dbCategory.id))
                }
            }
        }

        // Update database
        if (animeCategoriesToUpdate.isNotEmpty()) {
            animeHandler.await(true) {
                animes_categoriesQueries.deleteAnimeCategoryByAnimeId(anime.id)
                animeCategoriesToUpdate.forEach { (animeId, categoryId) ->
                    animes_categoriesQueries.insert(animeId, categoryId)
                }
            }
        }
    }

    /**
     * Restore history from Json
     *
     * @param history list containing history to be restored
     */
    private suspend fun restoreAnimeHistory(history: List<BackupAnimeHistory>) {
        // List containing history to be updated
        val toUpdate = mutableListOf<AnimeHistoryUpdate>()
        for ((url, lastSeen) in history) {
            var dbHistory = animeHandler.awaitOneOrNull {
                animehistoryQueries.getHistoryByEpisodeUrl(
                    url,
                )
            }
            // Check if history already in database and update
            if (dbHistory != null) {
                dbHistory = dbHistory.copy(
                    last_seen = Date(max(lastSeen, dbHistory.last_seen?.time ?: 0L)),
                )
                toUpdate.add(
                    AnimeHistoryUpdate(
                        episodeId = dbHistory.episode_id,
                        seenAt = dbHistory.last_seen!!,
                    ),
                )
            } else {
                // If not in database create
                animeHandler
                    .awaitOneOrNull { episodesQueries.getEpisodeByUrl(url) }
                    ?.let {
                        toUpdate.add(
                            AnimeHistoryUpdate(
                                episodeId = it._id,
                                seenAt = Date(lastSeen),
                            ),
                        )
                    }
            }
        }
        animeHandler.await(true) {
            toUpdate.forEach { payload ->
                animehistoryQueries.upsert(
                    payload.episodeId,
                    payload.seenAt,
                )
            }
        }
    }

    /**
     * Restores the sync of a manga.
     *
     * @param anime the anime whose sync have to be restored.
     * @param tracks the track list to restore.
     */
    private suspend fun restoreAnimeTracking(
        anime: Anime,
        tracks: List<tachiyomi.domain.track.anime.model.AnimeTrack>,
    ) {
        // Get tracks from database
        val dbTracks = animeHandler.awaitList { anime_syncQueries.getTracksByAnimeId(anime.id) }
        val toUpdate = mutableListOf<Anime_sync>()
        val toInsert = mutableListOf<tachiyomi.domain.track.anime.model.AnimeTrack>()

        tracks
            // Fix foreign keys with the current manga id
            .map { it.copy(animeId = anime.id) }
            .forEach { track ->
                var isInDatabase = false
                for (dbTrack in dbTracks) {
                    if (track.syncId == dbTrack.sync_id) {
                        // The sync is already in the db, only update its fields
                        var temp = dbTrack
                        if (track.remoteId != dbTrack.remote_id) {
                            temp = temp.copy(remote_id = track.remoteId)
                        }
                        if (track.libraryId != dbTrack.library_id) {
                            temp = temp.copy(library_id = track.libraryId)
                        }
                        temp = temp.copy(
                            last_episode_seen = max(
                                dbTrack.last_episode_seen,
                                track.lastEpisodeSeen,
                            ),
                        )
                        isInDatabase = true
                        toUpdate.add(temp)
                        break
                    }
                }
                if (!isInDatabase) {
                    // Insert new sync. Let the db assign the id
                    toInsert.add(track.copy(id = 0))
                }
            }

        // Update database
        if (toUpdate.isNotEmpty()) {
            animeHandler.await(true) {
                toUpdate.forEach { track ->
                    anime_syncQueries.update(
                        track.anime_id,
                        track.sync_id,
                        track.remote_id,
                        track.library_id,
                        track.title,
                        track.last_episode_seen,
                        track.total_episodes,
                        track.status,
                        track.score,
                        track.remote_url,
                        track.start_date,
                        track.finish_date,
                        track._id,
                    )
                }
            }
        }
        if (toInsert.isNotEmpty()) {
            animeHandler.await(true) {
                toInsert.forEach { track ->
                    anime_syncQueries.insert(
                        track.animeId,
                        track.syncId,
                        track.remoteId,
                        track.libraryId,
                        track.title,
                        track.lastEpisodeSeen,
                        track.totalEpisodes,
                        track.status,
                        track.score,
                        track.remoteUrl,
                        track.startDate,
                        track.finishDate,
                    )
                }
            }
        }
    }

    private fun restorePreferences(
        preferences: List<BackupPreference>,
        sharedPrefs: SharedPreferences,
    ) {
        MangaLibraryUpdateJob.setupTask(context)
        AnimeLibraryUpdateJob.setupTask(context)
        BackupCreateJob.setupTask(context)

        preferences.forEach { pref ->
            when (pref.value) {
                is IntPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is Int?) {
                        sharedPrefs.edit().putInt(pref.key, pref.value.value).apply()
                    }
                }
                is LongPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is Long?) {
                        sharedPrefs.edit().putLong(pref.key, pref.value.value).apply()
                    }
                }
                is FloatPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is Float?) {
                        sharedPrefs.edit().putFloat(pref.key, pref.value.value).apply()
                    }
                }
                is StringPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is String?) {
                        sharedPrefs.edit().putString(pref.key, pref.value.value).apply()
                    }
                }
                is BooleanPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is Boolean?) {
                        sharedPrefs.edit().putBoolean(pref.key, pref.value.value).apply()
                    }
                }
                is StringSetPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is Set<*>?) {
                        sharedPrefs.edit().putStringSet(pref.key, pref.value.value).apply()
                    }
                }
            }
        }
    }

    private fun restoreExtensionPreferences(prefs: List<BackupExtensionPreferences>) {
        prefs.forEach {
            val sharedPrefs = context.getSharedPreferences(it.name, 0x0)
            restorePreferences(it.prefs, sharedPrefs)
        }

        restoreProgress += 1
        showRestoreProgress(
            restoreProgress,
            restoreAmount,
            context.stringResource(MR.strings.extension_settings),
            context.stringResource(MR.strings.restoring_backup),
        )
    }

    private fun restoreExtensions(extensions: List<BackupExtension>) {
        extensions.forEach {
            if (context.packageManager.getInstalledPackages(0).none { pkg -> pkg.packageName == it.pkgName }) {
                logcat { it.pkgName }
                // save apk in files dir and open installer dialog
                val file = File(context.cacheDir, "${it.pkgName}.apk")
                file.writeBytes(it.apk)
                val intent = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(
                        file.getUriCompat(context),
                        "application/vnd.android.package-archive",
                    )
                    .setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                context.startActivity(intent)
            }
        }
    }

    // AM (CU) -->
    internal fun restoreEditedInfo(mangaJson: CustomMangaInfo?) {
        mangaJson ?: return
        setCustomMangaInfo.set(mangaJson)
    }
    // <-- AM (CU)

    /**
     * Called to update dialog in [BackupConst]
     *
     * @param progress restore progress
     * @param amount total restoreAmount of anime and manga
     * @param title title of restored anime and manga
     */
    private fun showRestoreProgress(progress: Int, amount: Int, title: String, contentTitle: String) {
        notifier.showRestoreProgress(title, contentTitle, progress, amount)
    }
}
