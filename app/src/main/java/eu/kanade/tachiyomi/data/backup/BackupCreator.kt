package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import androidx.preference.PreferenceManager
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.backup.BackupCreateFlags.BACKUP_CATEGORY
import eu.kanade.tachiyomi.data.backup.BackupCreateFlags.BACKUP_CHAPTER
import eu.kanade.tachiyomi.data.backup.BackupCreateFlags.BACKUP_CUSTOM_INFO
import eu.kanade.tachiyomi.data.backup.BackupCreateFlags.BACKUP_EXTENSIONS
import eu.kanade.tachiyomi.data.backup.BackupCreateFlags.BACKUP_EXT_PREFS
import eu.kanade.tachiyomi.data.backup.BackupCreateFlags.BACKUP_HISTORY
import eu.kanade.tachiyomi.data.backup.BackupCreateFlags.BACKUP_PREFS
import eu.kanade.tachiyomi.data.backup.BackupCreateFlags.BACKUP_TRACK
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeHistory
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupExtension
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionPreferences
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSerializer
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.backupAnimeTrackMapper
import eu.kanade.tachiyomi.data.backup.models.backupCategoryMapper
import eu.kanade.tachiyomi.data.backup.models.backupEpisodeMapper
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.source.anime.getPreferenceKey
import java.io.File
import java.io.FileOutputStream
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import okio.buffer
import okio.gzip
import okio.sink
import tachiyomi.core.i18n.stringResource
import tachiyomi.core.preference.Preference
import tachiyomi.core.util.system.logcat
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.anime.interactor.GetAnimeFavorites
import tachiyomi.domain.entries.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.history.anime.interactor.GetAnimeHistory
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BackupCreator(
    private val context: Context,
) {

    private val animeHandler: AnimeDatabaseHandler = Injekt.get()
    private val animeSourceManager: AnimeSourceManager = Injekt.get()
    private val getAnimeCategories: GetAnimeCategories = Injekt.get()
    private val getAnimeFavorites: GetAnimeFavorites = Injekt.get()
    private val getAnimeHistory: GetAnimeHistory = Injekt.get()

    // AM (CU) -->
    private val getCustomAnimeInfo: GetCustomAnimeInfo = Injekt.get()
    // <-- AM (CU)

    internal val parser = ProtoBuf

    /**
     * Create backup file from database
     *
     * @param uri path of Uri
     * @param isAutoBackup backup called from scheduled backup job
     */
    suspend fun createBackup(uri: Uri, flags: Int, isAutoBackup: Boolean): String {
        val databaseAnime = getAnimeFavorites.await()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val backup = Backup(
            backupAnimes(databaseAnime, flags),
            backupAnimeCategories(flags),
            emptyList(),
            prepAnimeExtensionInfoForSync(databaseAnime),
            backupPreferences(prefs, flags),
            backupExtensionPreferences(flags),
            backupExtensions(flags),
        )

        var file: UniFile? = null
        try {
            file = (
                if (isAutoBackup) {
                    // Get dir of file and create
                    val dir = UniFile.fromUri(context, uri)

                    // Delete older backups
                    dir?.listFiles { _, filename -> Backup.filenameRegex.matches(filename) }
                        .orEmpty()
                        .sortedByDescending { it.name }
                        .drop(MAX_AUTO_BACKUPS - 1)
                        .forEach { it.delete() }

                    // Create new file to place backup
                    dir?.createFile(Backup.getFilename())
                } else {
                    UniFile.fromUri(context, uri)
                }
                )
                ?: throw Exception(context.stringResource(MR.strings.create_backup_file_error))

            if (!file.isFile) {
                throw IllegalStateException("Failed to get handle on a backup file")
            }

            val byteArray = parser.encodeToByteArray(BackupSerializer, backup)
            if (byteArray.isEmpty()) {
                throw IllegalStateException(context.stringResource(MR.strings.empty_backup_error))
            }

            file.openOutputStream().also {
                // Force overwrite old file
                (it as? FileOutputStream)?.channel?.truncate(0)
            }.sink().gzip().buffer().use { it.write(byteArray) }
            val fileUri = file.uri

            // Make sure it's a valid backup file
            BackupFileValidator().validate(context, fileUri)

            return fileUri.toString()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            file?.delete()
            throw e
        }
    }

    private fun prepAnimeExtensionInfoForSync(animes: List<Anime>): List<BackupAnimeSource> {
        return animes
            .asSequence()
            .map(Anime::source)
            .distinct()
            .map(animeSourceManager::getOrStub)
            .map(BackupAnimeSource::copyFrom)
            .toList()
    }

    /**
     * Backup the categories of anime library
     *
     * @return list of [BackupCategory] to be backed up
     */
    private suspend fun backupAnimeCategories(options: Int): List<BackupCategory> {
        // Check if user wants category information in backup
        return if (options and BACKUP_CATEGORY == BACKUP_CATEGORY) {
            getAnimeCategories.await()
                .filterNot(Category::isSystemCategory)
                .map(backupCategoryMapper)
        } else {
            emptyList()
        }
    }

    private suspend fun backupAnimes(animes: List<Anime>, flags: Int): List<BackupAnime> {
        return animes.map {
            backupAnime(it, flags)
        }
    }

    /**
     * Convert an anime to Json
     *
     * @param anime anime that gets converted
     * @param options options for the backup
     * @return [BackupAnime] containing anime in a serializable form
     */
    private suspend fun backupAnime(anime: Anime, options: Int): BackupAnime {
        // Entry for this anime
        val animeObject = BackupAnime.copyFrom(
            anime,
            // AM (CU) -->
            if (options and BACKUP_CUSTOM_INFO == BACKUP_CUSTOM_INFO) getCustomAnimeInfo.get(anime.id) else null,
            // <-- AM (CU)
        )

        // Check if user wants chapter information in backup
        if (options and BACKUP_CHAPTER == BACKUP_CHAPTER) {
            // Backup all the chapters
            val episodes = animeHandler.awaitList {
                episodesQueries.getEpisodesByAnimeId(
                    anime.id,
                    backupEpisodeMapper,
                )
            }
            if (episodes.isNotEmpty()) {
                animeObject.episodes = episodes
            }
        }

        // Check if user wants category information in backup
        if (options and BACKUP_CATEGORY == BACKUP_CATEGORY) {
            // Backup categories for this manga
            val categoriesForAnime = getAnimeCategories.await(anime.id)
            if (categoriesForAnime.isNotEmpty()) {
                animeObject.categories = categoriesForAnime.map { it.order }
            }
        }

        // Check if user wants track information in backup
        if (options and BACKUP_TRACK == BACKUP_TRACK) {
            val tracks = animeHandler.awaitList {
                anime_syncQueries.getTracksByAnimeId(
                    anime.id,
                    backupAnimeTrackMapper,
                )
            }
            if (tracks.isNotEmpty()) {
                animeObject.tracking = tracks
            }
        }

        // Check if user wants history information in backup
        if (options and BACKUP_HISTORY == BACKUP_HISTORY) {
            val historyByAnimeId = getAnimeHistory.await(anime.id)
            if (historyByAnimeId.isNotEmpty()) {
                val history = historyByAnimeId.map { history ->
                    val episode = animeHandler.awaitOne {
                        episodesQueries.getEpisodeById(
                            history.episodeId,
                        )
                    }
                    BackupAnimeHistory(episode.url, history.seenAt?.time ?: 0L)
                }
                if (history.isNotEmpty()) {
                    animeObject.history = history
                }
            }
        }

        return animeObject
    }

    private fun backupExtensionPreferences(flags: Int): List<BackupExtensionPreferences> {
        if (flags and BACKUP_EXT_PREFS != BACKUP_EXT_PREFS) return emptyList()
        val prefs = mutableListOf<Pair<String, SharedPreferences>>()
        Injekt.get<AnimeSourceManager>().getCatalogueSources().forEach {
            val name = it.getPreferenceKey()
            prefs += Pair(name, context.getSharedPreferences(name, 0x0))
        }
        return prefs.map {
            BackupExtensionPreferences(
                it.first,
                backupPreferences(it.second, BACKUP_PREFS),
            )
        }
    }

    private fun backupExtensions(flags: Int): List<BackupExtension> {
        if (flags and BACKUP_EXTENSIONS != BACKUP_EXTENSIONS) return emptyList()
        val installedExtensions = mutableListOf<BackupExtension>()
        Injekt.get<AnimeExtensionManager>().installedExtensionsFlow.value.forEach {
            val packageName = it.pkgName
            val apk = File(
                context.packageManager
                    .getApplicationInfo(
                        packageName,
                        PackageManager.GET_META_DATA,
                    ).publicSourceDir,
            ).readBytes()
            installedExtensions.add(
                BackupExtension(packageName, apk),
            )
        }
        return installedExtensions
    }

    private fun backupPreferences(prefs: SharedPreferences, flags: Int): List<BackupPreference> {
        if (flags and BACKUP_PREFS != BACKUP_PREFS) return emptyList()
        val backupPreferences = mutableListOf<BackupPreference>()
        for (pref in prefs.all) {
            // AM (CONNECTIONS) -->
            if (pref.key.contains("connection")) continue
            // <-- AM (CONNECTIONS)
            val toAdd = when (pref.value) {
                is Int -> {
                    BackupPreference(pref.key, IntPreferenceValue(pref.value as Int))
                }
                is Long -> {
                    BackupPreference(pref.key, LongPreferenceValue(pref.value as Long))
                }
                is Float -> {
                    BackupPreference(pref.key, FloatPreferenceValue(pref.value as Float))
                }
                is String -> {
                    BackupPreference(pref.key, StringPreferenceValue(pref.value as String))
                }
                is Boolean -> {
                    BackupPreference(pref.key, BooleanPreferenceValue(pref.value as Boolean))
                }
                is Set<*> -> {
                    (pref.value as? Set<String>)?.let {
                        BackupPreference(pref.key, StringSetPreferenceValue(it))
                    } ?: continue
                }
                else -> {
                    continue
                }
            }
            backupPreferences.add(toAdd)
        }
        return backupPreferences.filter { !Preference.isPrivate(it.key) && !Preference.isAppState(it.key) }
    }
}

private val MAX_AUTO_BACKUPS: Int = 4
