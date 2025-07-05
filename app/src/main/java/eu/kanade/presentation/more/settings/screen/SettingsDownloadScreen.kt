package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastMap
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.OutlinedNumericChooser
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsDownloadScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_downloads

    @Composable
    override fun getPreferences(): List<Preference> {
        val getAnimeCategories = remember { Injekt.get<GetAnimeCategories>() }
        val allAnimeCategories by getAnimeCategories.subscribe().collectAsState(initial = emptyList())
        val downloadPreferences = remember { Injekt.get<DownloadPreferences>() }
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val speedLimit by downloadPreferences.downloadSpeedLimit().collectAsState()
        var currentSpeedLimit by remember { mutableIntStateOf(speedLimit) }
        var showDownloadLimitDialog by rememberSaveable { mutableStateOf(false) }
        if (showDownloadLimitDialog) {
            DownloadLimitDialog(
                initialValue = currentSpeedLimit,
                onDismissRequest = { showDownloadLimitDialog = false },
                onValueChanged = {
                    currentSpeedLimit = it
                },
                onConfirm = {
                    downloadPreferences.downloadSpeedLimit().set(currentSpeedLimit)
                    showDownloadLimitDialog = false
                },
            )
        }
        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = downloadPreferences.downloadOnlyOverWifi(),
                title = stringResource(MR.strings.connected_to_wifi),
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(AYMR.strings.download_speed_limit),
                subtitle = if (speedLimit == 0) {
                    stringResource(MR.strings.off)
                } else {
                    "$speedLimit KiB/s"
                },
                onClick = { showDownloadLimitDialog = true },
            ),
            Preference.PreferenceItem.ListPreference(
                preference = downloadPreferences.numberOfDownloads(),
                entries = (1..5).associateWith { it.toString() }.toImmutableMap(),
                title = stringResource(AYMR.strings.pref_download_slots),
            ),
            Preference.PreferenceItem.InfoPreference(stringResource(AYMR.strings.download_slots_info)),
            getDeleteEpisodesGroup(
                downloadPreferences = downloadPreferences,
                animeCategories = allAnimeCategories.toImmutableList(),
            ),
            getAutoDownloadGroup(
                downloadPreferences = downloadPreferences,
                allAnimeCategories = allAnimeCategories.toImmutableList(),
            ),
            getDownloadAheadGroup(downloadPreferences = downloadPreferences),
            getExternalDownloaderGroup(
                downloadPreferences = downloadPreferences,
                basePreferences = basePreferences,
            ),
        )
    }

    @Composable
    private fun getDeleteEpisodesGroup(
        downloadPreferences: DownloadPreferences,
        animeCategories: ImmutableList<Category>,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_delete_episodes),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.removeAfterMarkedAsSeen(),
                    title = stringResource(MR.strings.pref_remove_after_marked_as_seen),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.removeAfterSeenSlots(),
                    entries = persistentMapOf(
                        -1 to stringResource(MR.strings.disabled),
                        0 to stringResource(MR.strings.last_seen_episode),
                        1 to stringResource(MR.strings.second_to_last_episode),
                        2 to stringResource(MR.strings.third_to_last_episode),
                        3 to stringResource(MR.strings.fourth_to_last_episode),
                        4 to stringResource(MR.strings.fifth_to_last_episode),
                    ),
                    title = stringResource(AYMR.strings.pref_remove_after_seen),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.removeBookmarkedChapters(),
                    title = stringResource(MR.strings.pref_remove_bookmarked_episodes),
                ),
                getExcludedAnimeCategoriesPreference(
                    downloadPreferences = downloadPreferences,
                    categories = { animeCategories },
                ),
            ),
        )
    }

    @Composable
    private fun getExcludedAnimeCategoriesPreference(
        downloadPreferences: DownloadPreferences,
        categories: () -> List<Category>,
    ): Preference.PreferenceItem.MultiSelectListPreference {
        return Preference.PreferenceItem.MultiSelectListPreference(
            preference = downloadPreferences.removeExcludeAnimeCategories(),
            entries = categories()
                .associate { it.id.toString() to it.visualName }
                .toImmutableMap(),
            title = stringResource(AYMR.strings.pref_remove_exclude_categories_anime),
        )
    }

    @Composable
    private fun getAutoDownloadGroup(
        downloadPreferences: DownloadPreferences,
        allAnimeCategories: ImmutableList<Category>,
    ): Preference.PreferenceGroup {
        val downloadNewEpisodesPref = downloadPreferences.downloadNewEpisodes()
        val downloadNewUnseenEpisodesOnlyPref = downloadPreferences.downloadNewUnseenEpisodesOnly()
        val downloadNewEpisodeCategoriesPref = downloadPreferences.downloadNewEpisodeCategories()
        val downloadNewEpisodeCategoriesExcludePref = downloadPreferences.downloadNewEpisodeCategoriesExclude()

        val downloadNewEpisodes by downloadNewEpisodesPref.collectAsState()

        val includedAnime by downloadNewEpisodeCategoriesPref.collectAsState()
        val excludedAnime by downloadNewEpisodeCategoriesExcludePref.collectAsState()
        var showAnimeDialog by rememberSaveable { mutableStateOf(false) }
        if (showAnimeDialog) {
            TriStateListDialog(
                title = stringResource(AYMR.strings.anime_categories),
                message = stringResource(MR.strings.pref_download_new_categories_details),
                items = allAnimeCategories,
                initialChecked = includedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                initialInversed = excludedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showAnimeDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    downloadNewEpisodeCategoriesPref.set(
                        newIncluded.fastMap { it.id.toString() }.toSet(),
                    )
                    downloadNewEpisodeCategoriesExcludePref.set(
                        newExcluded.fastMap { it.id.toString() }.toSet(),
                    )
                    showAnimeDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_auto_download),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadNewEpisodesPref,
                    title = stringResource(AYMR.strings.pref_download_new_episodes),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadNewUnseenEpisodesOnlyPref,
                    title = stringResource(AYMR.strings.pref_download_new_unseen_episodes_only),
                    enabled = downloadNewEpisodes,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(AYMR.strings.anime_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allAnimeCategories,
                        included = includedAnime,
                        excluded = excludedAnime,
                    ),
                    enabled = downloadNewEpisodes,
                    onClick = { showAnimeDialog = true },
                ),
            ),
        )
    }

    @Composable
    private fun getDownloadAheadGroup(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.download_ahead),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.autoDownloadWhileWatching(),
                    entries = listOf(0, 2, 3, 5, 10)
                        .associateWith {
                            if (it == 0) {
                                stringResource(MR.strings.disabled)
                            } else {
                                pluralStringResource(AYMR.plurals.next_unseen_episodes, count = it, it)
                            }
                        }
                        .toImmutableMap(),
                    title = stringResource(AYMR.strings.auto_download_while_watching),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.autoDownloadWhileReading(),
                    entries = listOf(0, 2, 3, 5, 10)
                        .associateWith {
                            if (it == 0) {
                                stringResource(MR.strings.disabled)
                            } else {
                                pluralStringResource(MR.plurals.next_unread_chapters, count = it, it)
                            }
                        }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.auto_download_while_reading),
                ),
                Preference.PreferenceItem.InfoPreference(
                    stringResource(AYMR.strings.download_ahead_info),
                ),
            ),
        )
    }

    @Composable
    private fun getExternalDownloaderGroup(
        downloadPreferences: DownloadPreferences,
        basePreferences: BasePreferences,
    ): Preference.PreferenceGroup {
        val useExternalDownloader = downloadPreferences.useExternalDownloader()
        val externalDownloaderPreference = downloadPreferences.externalDownloaderSelection()

        val pm = basePreferences.context.packageManager
        val installedPackages = pm.getInstalledPackages(0)
        val supportedDownloaders = installedPackages.filter {
            when (it.packageName) {
                "idm.internet.download.manager" -> true
                "idm.internet.download.manager.plus" -> true
                "idm.internet.download.manager.adm.lite" -> true
                "com.dv.adm" -> true
                else -> false
            }
        }
        val packageNames = supportedDownloaders.map { it.packageName }
        val packageNamesReadable = supportedDownloaders
            .map { pm.getApplicationLabel(it.applicationInfo!!).toString() }

        val packageNamesMap: Map<String, String> =
            mapOf("" to "None") + packageNames.zip(packageNamesReadable).toMap()

        return Preference.PreferenceGroup(
            title = stringResource(AYMR.strings.pref_category_external_downloader),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = useExternalDownloader,
                    title = stringResource(AYMR.strings.pref_use_external_downloader),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = externalDownloaderPreference,
                    entries = packageNamesMap.toPersistentMap(),
                    title = stringResource(AYMR.strings.pref_external_downloader_selection),
                ),
            ),
        )
    }

    @Composable
    private fun DownloadLimitDialog(
        initialValue: Int,
        onDismissRequest: () -> Unit,
        onValueChanged: (newValue: Int) -> Unit,
        onConfirm: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(AYMR.strings.download_speed_limit)) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .padding(bottom = MaterialTheme.padding.medium)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        OutlinedNumericChooser(
                            label = stringResource(AYMR.strings.download_speed_limit),
                            placeholder = "0",
                            suffix = "KiB/s",
                            value = initialValue,
                            step = 100,
                            min = 0,
                            onValueChanged = onValueChanged,
                        )
                    }
                    Text(text = stringResource(AYMR.strings.download_speed_limit_hint))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
        )
    }
}
