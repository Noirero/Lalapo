package eu.kanade.tachiyomi

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.domain.backup.service.BackupPreferences
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.connections.service.ConnectionsPreferences
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.backup.BackupCreatorJob
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.MANGA_NON_COMPLETED
import eu.kanade.tachiyomi.data.preference.PreferenceValues
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.updater.AppUpdateJob
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionUpdateJob
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.util.preference.minusAssign
import eu.kanade.tachiyomi.util.preference.plusAssign
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @return true if a migration is performed, false otherwise.
     */
    fun upgrade(
        context: Context,
        preferenceStore: PreferenceStore,
        basePreferences: BasePreferences,
        uiPreferences: UiPreferences,
        networkPreferences: NetworkPreferences,
        sourcePreferences: SourcePreferences,
        securityPreferences: SecurityPreferences,
        libraryPreferences: LibraryPreferences,
        playerPreferences: PlayerPreferences,
        backupPreferences: BackupPreferences,
        // AM (CN) -->
        connectionsPreferences: ConnectionsPreferences,
        connectionsManager: ConnectionsManager,
        // <-- AM (CN)
    ): Boolean {
        val lastVersionCode = preferenceStore.getInt("last_version_code", 0)
        val oldVersion = lastVersionCode.get()
        if (oldVersion < BuildConfig.VERSION_CODE) {
            lastVersionCode.set(BuildConfig.VERSION_CODE)

            // Always set up background tasks to ensure they're running
            if (BuildConfig.INCLUDE_UPDATER) {
                AppUpdateJob.setupTask(context)
            }
            AnimeExtensionUpdateJob.setupTask(context)
            AnimeLibraryUpdateJob.setupTask(context)
            BackupCreatorJob.setupTask(context)

            // Fresh install
            if (oldVersion == 0) {
                return false
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (oldVersion < 15) {
                // Delete internal chapter cache dir.
                File(context.cacheDir, "chapter_disk_cache").deleteRecursively()
            }
            if (oldVersion < 19) {
                // Move covers to external files dir.
                val oldDir = File(context.externalCacheDir, "cover_disk_cache")
                if (oldDir.exists()) {
                    val destDir = context.getExternalFilesDir("covers")
                    if (destDir != null) {
                        oldDir.listFiles()?.forEach {
                            it.renameTo(File(destDir, it.name))
                        }
                    }
                }
            }
            if (oldVersion < 26) {
                // Delete external chapter cache dir.
                val extCache = context.externalCacheDir
                if (extCache != null) {
                    val chapterCache = File(extCache, "chapter_disk_cache")
                    if (chapterCache.exists()) {
                        chapterCache.deleteRecursively()
                    }
                }
            }
            if (oldVersion < 44) {
                // Reset sorting preference if using removed sort by source
                val oldSortingMode = prefs.getInt(libraryPreferences.librarySortingMode().key(), 0)

                if (oldSortingMode == 5) { // SOURCE = 5
                    prefs.edit {
                        putInt(libraryPreferences.librarySortingMode().key(), 0) // ALPHABETICAL = 0
                    }
                }
            }
            if (oldVersion < 52) {
                // Migrate library filters to tri-state versions
                fun convertBooleanPrefToTriState(key: String): Int {
                    val oldPrefValue = prefs.getBoolean(key, false)
                    return if (oldPrefValue) {
                        ExtendedNavigationView.Item.TriStateGroup.State.INCLUDE.value
                    } else {
                        ExtendedNavigationView.Item.TriStateGroup.State.IGNORE.value
                    }
                }
            }
            if (oldVersion < 54) {
                // Force MAL log out due to login flow change
                // v52: switched from scraping to WebView
                // v53: switched from WebView to OAuth
                val trackManager = Injekt.get<TrackManager>()
                if (trackManager.myAnimeList.isLogged) {
                    trackManager.myAnimeList.logout()
                    context.toast(R.string.myanimelist_relogin)
                }
            }
            if (oldVersion < 57) {
                // Migrate DNS over HTTPS setting
                val wasDohEnabled = prefs.getBoolean("enable_doh", false)
                if (wasDohEnabled) {
                    prefs.edit {
                        putInt(networkPreferences.dohProvider().key(), PREF_DOH_CLOUDFLARE)
                        remove("enable_doh")
                    }
                }
            }
            if (oldVersion < 59) {
                // Reset rotation to Free after replacing Lock
                if (prefs.contains("pref_rotation_type_key")) {
                    prefs.edit {
                        putInt("pref_rotation_type_key", 1)
                    }
                }

                // Disable update check for Android 5.x users
                if (BuildConfig.INCLUDE_UPDATER && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                    AppUpdateJob.cancelTask(context)
                }
            }
            if (oldVersion < 61) {
                // Handle removed every 1 or 2 hour library updates
                val animeupdateInterval = libraryPreferences.libraryUpdateInterval().get()
                if (animeupdateInterval == 1 || animeupdateInterval == 2) {
                    libraryPreferences.libraryUpdateInterval().set(3)
                    AnimeLibraryUpdateJob.setupTask(context, 3)
                }
            }
            if (oldVersion < 64) {
                // Set up background tasks
                if (BuildConfig.INCLUDE_UPDATER) {
                    AppUpdateJob.setupTask(context)
                }
                AnimeExtensionUpdateJob.setupTask(context)
                AnimeLibraryUpdateJob.setupTask(context)
            }
            if (oldVersion < 64) {
                val oldSortingMode = prefs.getInt(libraryPreferences.librarySortingMode().key(), 0)
                val oldSortingDirection = prefs.getBoolean("library_sorting_ascending", true)

                val newSortingMode = when (oldSortingMode) {
                    0 -> "ALPHABETICAL"
                    1 -> "LAST_READ"
                    2 -> "LAST_CHECKED"
                    3 -> "UNREAD"
                    4 -> "TOTAL_CHAPTERS"
                    6 -> "LATEST_CHAPTER"
                    8 -> "DATE_FETCHED"
                    7 -> "DATE_ADDED"
                    else -> "ALPHABETICAL"
                }

                val newSortingDirection = when (oldSortingDirection) {
                    true -> "ASCENDING"
                    else -> "DESCENDING"
                }

                prefs.edit(commit = true) {
                    remove(libraryPreferences.librarySortingMode().key())
                    remove("library_sorting_ascending")
                }

                prefs.edit {
                    putString(libraryPreferences.librarySortingMode().key(), newSortingMode)
                    putString("library_sorting_ascending", newSortingDirection)
                }
            }
            if (oldVersion < 70) {
                if (sourcePreferences.enabledLanguages().isSet()) {
                    sourcePreferences.enabledLanguages() += "all"
                }
            }
            if (oldVersion < 71) {
                // Handle removed every 3, 4, 6, and 8 hour library updates
                val updateInterval = libraryPreferences.libraryUpdateInterval().get()
                if (updateInterval in listOf(3, 4, 6, 8)) {
                    libraryPreferences.libraryUpdateInterval().set(12)
                    AnimeLibraryUpdateJob.setupTask(context, 12)
                }
            }
            if (oldVersion < 72) {
                val oldUpdateOngoingOnly = prefs.getBoolean("pref_update_only_non_completed_key", true)
                if (!oldUpdateOngoingOnly) {
                    libraryPreferences.libraryUpdateItemRestriction() -= MANGA_NON_COMPLETED
                }
            }
            if (oldVersion < 75) {
                val oldSecureScreen = prefs.getBoolean("secure_screen", false)
                if (oldSecureScreen) {
                    securityPreferences.secureScreen().set(SecurityPreferences.SecureScreenMode.ALWAYS)
                }
                if (DeviceUtil.isMiui && basePreferences.extensionInstaller().get() == PreferenceValues.ExtensionInstaller.PACKAGEINSTALLER) {
                    basePreferences.extensionInstaller().set(PreferenceValues.ExtensionInstaller.LEGACY)
                }
            }
            if (oldVersion < 76) {
                BackupCreatorJob.setupTask(context)
            }
            if (oldVersion < 81) {
                // Handle renamed enum values
                prefs.edit {
                    val newSortingMode = when (val oldSortingMode = prefs.getString(libraryPreferences.librarySortingMode().key(), "ALPHABETICAL")) {
                        "LAST_CHECKED" -> "LAST_MANGA_UPDATE"
                        "UNREAD" -> "UNREAD_COUNT"
                        "DATE_FETCHED" -> "CHAPTER_FETCH_DATE"
                        else -> oldSortingMode
                    }
                    putString(libraryPreferences.librarySortingMode().key(), newSortingMode)
                }
            }
            if (oldVersion < 82) {
                prefs.edit {
                    val sort = prefs.getString(libraryPreferences.librarySortingMode().key(), null) ?: return@edit
                    val direction = prefs.getString("library_sorting_ascending", "ASCENDING")!!
                    putString(libraryPreferences.librarySortingMode().key(), "$sort,$direction")
                    remove("library_sorting_ascending")
                }
            }
            if (oldVersion < 84) {
                if (backupPreferences.numberOfBackups().get() == 1) {
                    backupPreferences.numberOfBackups().set(2)
                }
                if (backupPreferences.backupInterval().get() == 0) {
                    backupPreferences.backupInterval().set(12)
                    BackupCreatorJob.setupTask(context)
                }
            }
            if (oldVersion < 85) {
                val preferences = listOf(
                    libraryPreferences.filterEpisodeBySeen(),
                    libraryPreferences.filterEpisodeByDownloaded(),
                    libraryPreferences.filterEpisodeByBookmarked(),
                    // AM (FM) -->
                    libraryPreferences.filterEpisodeByFillermarked(),
                    // <-- AM (FM)
                    libraryPreferences.sortEpisodeBySourceOrNumber(),
                    libraryPreferences.displayEpisodeByNameOrNumber(),
                    libraryPreferences.sortEpisodeByAscendingOrDescending(),
                )

                prefs.edit {
                    preferences.forEach { preference ->
                        val key = preference.key()
                        val value = prefs.getInt(key, Int.MIN_VALUE)
                        if (value == Int.MIN_VALUE) return@forEach
                        remove(key)
                        putLong(key, value.toLong())
                    }
                }
            }
            if (oldVersion < 86) {
                if (uiPreferences.themeMode().isSet()) {
                    prefs.edit {
                        val themeMode = prefs.getString(uiPreferences.themeMode().key(), null) ?: return@edit
                        putString(uiPreferences.themeMode().key(), themeMode.uppercase())
                    }
                }
                // AM (DC) -->
                if (connectionsPreferences.discordRPCStatus().isSet()) {
                    prefs.edit {
                        val oldString = try {
                            prefs.getString(connectionsPreferences.discordRPCStatus().key(), null)
                        } catch (e: ClassCastException) {
                            null
                        } ?: return@edit
                        val newInt = when (oldString) {
                            "dnd" -> -1
                            "idle" -> 0
                            else -> 1
                        }
                        putInt(connectionsPreferences.discordRPCStatus().key(), newInt)
                    }
                }

                if(connectionsPreferences.connectionsToken(connectionsManager.discord).get().isNotBlank()) {
                    connectionsPreferences.setConnectionsCredentials(connectionsManager.discord, "Discord", "Logged In")
                }
                // <-- AM (DC)
            }
            if (oldVersion < 92) {
                if (playerPreferences.progressPreference().isSet()) {
                    prefs.edit {
                        val progressString = try {
                            prefs.getString(playerPreferences.progressPreference().key(), null)
                        } catch (e: ClassCastException) {
                            null
                        } ?: return@edit
                        val newProgress = progressString.toFloatOrNull() ?: return@edit
                        putFloat(playerPreferences.progressPreference().key(), newProgress)
                    }
                }
            }
            if (oldVersion < 93) {
                listOf(
                    playerPreferences.defaultPlayerOrientationType(),
                    playerPreferences.defaultPlayerOrientationLandscape(),
                    playerPreferences.defaultPlayerOrientationPortrait(),
                    playerPreferences.skipLengthPreference(),
                ).forEach { pref ->
                    if (pref.isSet()) {
                        prefs.edit {
                            val oldString = try {
                                prefs.getString(pref.key(), null)
                            } catch (e: ClassCastException) {
                                null
                            } ?: return@edit
                            val newInt = oldString.toIntOrNull() ?: return@edit
                            putInt(pref.key(), newInt)
                        }
                        val trackingQueuePref =
                            context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)
                        trackingQueuePref.all.forEach {
                            val (_, lastChapterRead) = it.value.toString().split(":")
                            trackingQueuePref.edit {
                                remove(it.key)
                                putFloat(it.key, lastChapterRead.toFloat())
                            }
                        }
                    }
                    return true
                }
            }
        }
        return false
    }
}
