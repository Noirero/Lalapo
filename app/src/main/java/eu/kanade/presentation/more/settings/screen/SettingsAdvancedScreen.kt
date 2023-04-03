package eu.kanade.presentation.more.settings.screen

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.entries.anime.interactor.GetAllAnime
import eu.kanade.domain.items.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.EpisodeCache
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadCache
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferenceValues
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_360
import eu.kanade.tachiyomi.network.PREF_DOH_ADGUARD
import eu.kanade.tachiyomi.network.PREF_DOH_ALIDNS
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import eu.kanade.tachiyomi.network.PREF_DOH_CONTROLD
import eu.kanade.tachiyomi.network.PREF_DOH_DNSPOD
import eu.kanade.tachiyomi.network.PREF_DOH_GOOGLE
import eu.kanade.tachiyomi.network.PREF_DOH_MULLVAD
import eu.kanade.tachiyomi.network.PREF_DOH_NJALLA
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD101
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD9
import eu.kanade.tachiyomi.network.PREF_DOH_SHECAN
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isPackageInstalled
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.powerManager
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import logcat.LogPriority
import okhttp3.Headers
import rikka.sui.Sui
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

object SettingsAdvancedScreen : SearchableSettings {
    @ReadOnlyComposable
    @Composable
    @StringRes
    override fun getTitleRes() = R.string.pref_category_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val networkPreferences = remember { Injekt.get<NetworkPreferences>() }

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                pref = basePreferences.acraEnabled(),
                title = stringResource(R.string.pref_enable_acra),
                subtitle = stringResource(R.string.pref_acra_summary),
                enabled = false, // acra is disabled
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.pref_dump_crash_logs),
                subtitle = stringResource(R.string.pref_dump_crash_logs_summary),
                onClick = {
                    scope.launch {
                        CrashLogUtil(context).dumpLogs()
                    }
                },
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = networkPreferences.verboseLogging(),
                title = stringResource(R.string.pref_verbose_logging),
                subtitle = stringResource(R.string.pref_verbose_logging_summary),
                onValueChanged = {
                    context.toast(R.string.requires_app_restart)
                    true
                },
            ),
            getBackgroundActivityGroup(),
            getDataGroup(),
            getNetworkGroup(networkPreferences = networkPreferences),
            getLibraryGroup(),
            getExtensionsGroup(basePreferences = basePreferences),
            // AM (CU) -->
            getDownloaderGroup(),
            // <-- AM (CU)
        )
    }

    @Composable
    private fun getBackgroundActivityGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val navigator = LocalNavigator.currentOrThrow

        return Preference.PreferenceGroup(
            title = stringResource(R.string.label_background_activity),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_disable_battery_optimization),
                    subtitle = stringResource(R.string.pref_disable_battery_optimization_summary),
                    onClick = {
                        val packageName: String = context.packageName
                        if (!context.powerManager.isIgnoringBatteryOptimizations(packageName)) {
                            try {
                                @SuppressLint("BatteryLife")
                                val intent = Intent().apply {
                                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    data = "package:$packageName".toUri()
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                context.toast(R.string.battery_optimization_setting_activity_not_found)
                            }
                        } else {
                            context.toast(R.string.battery_optimization_disabled)
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Don't kill my app!",
                    subtitle = stringResource(R.string.about_dont_kill_my_app),
                    onClick = { uriHandler.openUri("https://dontkillmyapp.com/") },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_worker_info),
                    onClick = { navigator.push(WorkerInfoScreen) },
                ),
            ),
        )
    }

    @Composable
    private fun getDataGroup(): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }

        val episodeCache = remember { Injekt.get<EpisodeCache>() }
        var readableSizeSema by remember { mutableStateOf(0) }
        val readableAnimeSize = remember(readableSizeSema) { episodeCache.readableSize }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.label_data),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_episode_cache),
                    subtitle = stringResource(R.string.used_cache, readableAnimeSize),
                    onClick = {
                        scope.launchNonCancellable {
                            try {
                                val deletedFiles = episodeCache.clear()
                                withUIContext {
                                    context.toast(context.getString(R.string.cache_deleted, deletedFiles))
                                    readableSizeSema++
                                }
                            } catch (e: Throwable) {
                                logcat(LogPriority.ERROR, e)
                                withUIContext { context.toast(R.string.cache_delete_error) }
                            }
                        }
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.autoClearItemCache(),
                    title = stringResource(R.string.pref_auto_clear_episode_cache),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_invalidate_download_cache),
                    subtitle = stringResource(R.string.pref_invalidate_episode_download_cache_summary),
                    onClick = {
                        Injekt.get<AnimeDownloadCache>().invalidateCache()
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_anime_database),
                    subtitle = stringResource(R.string.pref_clear_anime_database_summary),
                    onClick = { navigator.push(ClearAnimeDatabaseScreen()) },
                ),
            ),
        )
    }

    @Composable
    private fun getNetworkGroup(
        networkPreferences: NetworkPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val networkHelper = remember { Injekt.get<NetworkHelper>() }

        val userAgentPref = networkPreferences.defaultUserAgent()
        val userAgent by userAgentPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(R.string.label_network),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_cookies),
                    onClick = {
                        networkHelper.cookieManager.removeAll()
                        context.toast(R.string.cookies_cleared)
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_clear_webview_data),
                    onClick = {
                        try {
                            WebView(context).run {
                                setDefaultSettings()
                                clearCache(true)
                                clearFormData()
                                clearHistory()
                                clearSslPreferences()
                            }
                            WebStorage.getInstance().deleteAllData()
                            context.applicationInfo?.dataDir?.let { File("$it/app_webview/").deleteRecursively() }
                            context.toast(R.string.webview_data_deleted)
                        } catch (e: Throwable) {
                            logcat(LogPriority.ERROR, e)
                            context.toast(R.string.cache_delete_error)
                        }
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = networkPreferences.dohProvider(),
                    title = stringResource(R.string.pref_dns_over_https),
                    entries = mapOf(
                        -1 to stringResource(R.string.disabled),
                        PREF_DOH_CLOUDFLARE to "Cloudflare",
                        PREF_DOH_GOOGLE to "Google",
                        PREF_DOH_ADGUARD to "AdGuard",
                        PREF_DOH_QUAD9 to "Quad9",
                        PREF_DOH_ALIDNS to "AliDNS",
                        PREF_DOH_DNSPOD to "DNSPod",
                        PREF_DOH_360 to "360",
                        PREF_DOH_QUAD101 to "Quad 101",
                        PREF_DOH_MULLVAD to "Mullvad",
                        PREF_DOH_CONTROLD to "Control D",
                        PREF_DOH_NJALLA to "Njalla",
                        PREF_DOH_SHECAN to "Shecan",
                    ),
                    onValueChanged = {
                        context.toast(R.string.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.EditTextPreference(
                    pref = userAgentPref,
                    title = stringResource(R.string.pref_user_agent_string),
                    onValueChanged = {
                        if (it.isBlank()) {
                            context.toast(R.string.error_user_agent_string_blank)
                            return@EditTextPreference false
                        }
                        try {
                            // OkHttp checks for valid values internally
                            Headers.Builder().add("User-Agent", it)
                        } catch (_: IllegalArgumentException) {
                            context.toast(R.string.error_user_agent_string_invalid)
                            return@EditTextPreference false
                        }
                        context.toast(R.string.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_reset_user_agent_string),
                    enabled = remember(userAgent) { userAgent != userAgentPref.defaultValue() },
                    onClick = {
                        userAgentPref.delete()
                        context.toast(R.string.requires_app_restart)
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getLibraryGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val trackManager = remember { Injekt.get<TrackManager>() }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.label_library),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_refresh_library_covers),
                    onClick = { AnimeLibraryUpdateService.start(context, target = AnimeLibraryUpdateService.Target.COVERS) },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_refresh_library_tracking),
                    subtitle = stringResource(R.string.pref_refresh_library_tracking_summary),
                    enabled = trackManager.hasLoggedServices(),
                    onClick = { AnimeLibraryUpdateService.start(context, target = AnimeLibraryUpdateService.Target.TRACKING) },
                ),
            ),
        )
    }

    @Composable
    private fun getExtensionsGroup(
        basePreferences: BasePreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        var shizukuMissing by rememberSaveable { mutableStateOf(false) }

        if (shizukuMissing) {
            val dismiss = { shizukuMissing = false }
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text(text = stringResource(R.string.ext_installer_shizuku)) },
                text = { Text(text = stringResource(R.string.ext_installer_shizuku_unavailable_dialog)) },
                dismissButton = {
                    TextButton(onClick = dismiss) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dismiss()
                            uriHandler.openUri("https://shizuku.rikka.app/download")
                        },
                    ) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                },
            )
        }
        return Preference.PreferenceGroup(
            title = stringResource(R.string.label_extensions),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    pref = basePreferences.extensionInstaller(),
                    title = stringResource(R.string.ext_installer_pref),
                    entries = PreferenceValues.ExtensionInstaller.values()
                        .run {
                            if (DeviceUtil.isMiui) {
                                filter { it != PreferenceValues.ExtensionInstaller.PACKAGEINSTALLER }
                            } else {
                                toList()
                            }
                        }.associateWith { stringResource(it.titleResId) },
                    onValueChanged = {
                        if (it == PreferenceValues.ExtensionInstaller.SHIZUKU &&
                            !(context.isPackageInstalled("moe.shizuku.privileged.api") || Sui.isSui())
                        ) {
                            shizukuMissing = true
                            false
                        } else {
                            true
                        }
                    },
                ),
            ),
        )
    }

    // AM (CU) -->
    @Composable
    fun CleanupDownloadsDialog(
        onDismissRequest: () -> Unit,
        onCleanupDownloads: (removeRead: Boolean, removeNonFavorite: Boolean) -> Unit,
    ) {
        val context = LocalContext.current
        val options = remember { context.resources.getStringArray(R.array.clean_up_downloads).toList() }
        val selection = remember {
            options.toMutableStateList()
        }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(R.string.clean_up_downloaded_episodes)) },
            text = {
                LazyColumn {
                    options.forEachIndexed { index, option ->
                        item {
                            val isSelected = index == 0 || selection.contains(option)
                            val onSelectionChanged = {
                                when (!isSelected) {
                                    true -> selection.add(option)
                                    false -> selection.remove(option)
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectionChanged() },
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onSelectionChanged() },
                                )
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = true,
            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        val removeRead = options[1] in selection
                        val removeNonFavorite = options[2] in selection
                        onCleanupDownloads(removeRead, removeNonFavorite)
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        )
    }

    @Composable
    private fun getDownloaderGroup(): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            CleanupDownloadsDialog(
                onDismissRequest = { dialogOpen = false },
                onCleanupDownloads = { removeRead, removeNonFavorite ->
                    dialogOpen = false
                    if (job?.isActive == true) return@CleanupDownloadsDialog
                    context.toast(R.string.starting_cleanup)
                    job = scope.launchNonCancellable {
                        val animeList = Injekt.get<GetAllAnime>().await()
                        val downloadManager: AnimeDownloadManager = Injekt.get()
                        var foldersCleared = 0
                        Injekt.get<AnimeSourceManager>().getOnlineSources().forEach { source ->
                            val animeFolders = downloadManager.getAnimeFolders(source)
                            val sourceAnime = animeList
                                .asSequence()
                                .filter { it.source == source.id }
                                // AM (CU)>
                                .map { it to DiskUtil.buildValidFilename(it.ogTitle) }
                                .toList()

                            animeFolders.forEach mangaFolder@{ animeFolder ->
                                val anime =
                                    sourceAnime.find { (_, folderName) -> folderName == animeFolder.name }?.first
                                if (anime == null) {
                                    // download is orphaned delete it
                                    foldersCleared += 1 + (
                                        animeFolder.listFiles()
                                            .orEmpty().size
                                        )
                                    animeFolder.delete()
                                } else {
                                    val episodeList = Injekt.get<GetEpisodeByAnimeId>().await(anime.id)
                                    foldersCleared += downloadManager.cleanupEpisodes(
                                        episodeList,
                                        anime,
                                        source,
                                        removeRead,
                                        removeNonFavorite,
                                    )
                                }
                            }
                        }
                        withUIContext {
                            val cleanupString =
                                if (foldersCleared == 0) {
                                    context.getString(R.string.no_folders_to_cleanup)
                                } else {
                                    context.resources!!.getQuantityString(
                                        R.plurals.cleanup_done,
                                        foldersCleared,
                                        foldersCleared,
                                    )
                                }
                            context.toast(cleanupString, Toast.LENGTH_LONG)
                        }
                    }
                },
            )
        }
        return Preference.PreferenceGroup(
            title = stringResource(R.string.download_notifier_downloader_title),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.clean_up_downloaded_episodes),
                    subtitle = stringResource(R.string.delete_unused_episodes),
                    onClick = { dialogOpen = true },
                ),
            ),
        )
    }
    private var job: Job? = null
    // <-- AM (CU)
}
