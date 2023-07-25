package tachiyomi.domain.library.service

import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum
import tachiyomi.domain.entries.TriStateFilter
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.anime.model.AnimeLibrarySort
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroup

class LibraryPreferences(
    private val preferenceStore: PreferenceStore,
) {

    // Common options

    fun libraryDisplayMode() = preferenceStore.getObject("pref_display_mode_library", LibraryDisplayMode.default, LibraryDisplayMode.Serializer::serialize, LibraryDisplayMode.Serializer::deserialize)

    fun libraryAnimeSortingMode() = preferenceStore.getObject("animelib_sorting_mode", AnimeLibrarySort.default, AnimeLibrarySort.Serializer::serialize, AnimeLibrarySort.Serializer::deserialize)

    fun libraryUpdateInterval() = preferenceStore.getInt("pref_library_update_interval_key", 0)

    fun libraryUpdateLastTimestamp() = preferenceStore.getLong("library_update_last_timestamp", 0L)

    fun libraryUpdateDeviceRestriction() = preferenceStore.getStringSet(
        "library_update_restriction",
        setOf(
            DEVICE_ONLY_ON_WIFI,
        ),
    )
    fun libraryUpdateItemRestriction() = preferenceStore.getStringSet(
        "library_update_manga_restriction",
        setOf(
            ENTRY_HAS_UNVIEWED,
            ENTRY_NON_COMPLETED,
            ENTRY_NON_VIEWED,
        ),
    )

    fun autoUpdateMetadata() = preferenceStore.getBoolean("auto_update_metadata", false)

    fun autoUpdateTrackers() = preferenceStore.getBoolean("auto_update_trackers", false)

    fun showContinueViewingButton() = preferenceStore.getBoolean("display_continue_reading_button", false)

    // Common Category

    fun categoryTabs() = preferenceStore.getBoolean("display_category_tabs", true)

    fun categoryNumberOfItems() = preferenceStore.getBoolean("display_number_of_items", false)

    fun categorizedDisplaySettings() = preferenceStore.getBoolean("categorized_display", false)

    // Common badges

    fun downloadBadge() = preferenceStore.getBoolean("display_download_badge", false)

    fun localBadge() = preferenceStore.getBoolean("display_local_badge", true)

    fun languageBadge() = preferenceStore.getBoolean("display_language_badge", false)

    fun newShowUpdatesCount() = preferenceStore.getBoolean("library_show_updates_count", true)

    // Common Cache

    fun autoClearItemCache() = preferenceStore.getBoolean("auto_clear_chapter_cache", false)

    // Mixture Columns

    fun animePortraitColumns() = preferenceStore.getInt("pref_animelib_columns_portrait_key", 0)

    fun animeLandscapeColumns() = preferenceStore.getInt("pref_animelib_columns_landscape_key", 0)

    // Mixture Filter

    fun filterDownloadedAnime() = preferenceStore.getEnum("pref_filter_animelib_downloaded_v2", TriStateFilter.DISABLED)

    fun filterUnseen() = preferenceStore.getEnum("pref_filter_animelib_unread_v2", TriStateFilter.DISABLED)

    fun filterStartedAnime() = preferenceStore.getEnum("pref_filter_animelib_started_v2", TriStateFilter.DISABLED)

    fun filterBookmarkedAnime() = preferenceStore.getEnum("pref_filter_animelib_bookmarked_v2", TriStateFilter.DISABLED)

    // AM (FM) -->
    fun filterFillermarkedAnime() = preferenceStore.getEnum("pref_filter_animelib_fillermarked_v2", TriStateFilter.DISABLED)
    // <-- AM (FM)

    fun filterCompletedAnime() = preferenceStore.getEnum("pref_filter_animelib_completed_v2", TriStateFilter.DISABLED)

    fun filterTrackedAnime(id: Int) = preferenceStore.getEnum("pref_filter_animelib_tracked_${id}_v2", TriStateFilter.DISABLED)

    // Mixture Update Count

    fun newAnimeUpdatesCount() = preferenceStore.getInt("library_unseen_updates_count", 0)

    // Mixture Category

    fun defaultAnimeCategory() = preferenceStore.getInt("default_anime_category", -1)

    fun lastUsedAnimeCategory() = preferenceStore.getInt("last_used_anime_category", 0)

    fun animeLibraryUpdateCategories() = preferenceStore.getStringSet("animelib_update_categories", emptySet())

    fun animeLibraryUpdateCategoriesExclude() = preferenceStore.getStringSet("animelib_update_categories_exclude", emptySet())

    // Mixture Item

    fun filterEpisodeBySeen() = preferenceStore.getLong("default_episode_filter_by_seen", Anime.SHOW_ALL)

    fun filterEpisodeByDownloaded() = preferenceStore.getLong("default_episode_filter_by_downloaded", Anime.SHOW_ALL)

    fun filterEpisodeByBookmarked() = preferenceStore.getLong("default_episode_filter_by_bookmarked", Anime.SHOW_ALL)

    // AM (FM) -->
    fun filterEpisodeByFillermarked() = preferenceStore.getLong("default_episode_filter_by_fillermarked", Anime.SHOW_ALL)
    // <-- AM (FM)

    // and upload date
    fun sortEpisodeBySourceOrNumber() = preferenceStore.getLong("default_episode_sort_by_source_or_number", Anime.EPISODE_SORTING_SOURCE)

    fun displayEpisodeByNameOrNumber() = preferenceStore.getLong("default_chapter_display_by_name_or_number", Anime.EPISODE_DISPLAY_NAME)

    fun sortEpisodeByAscendingOrDescending() = preferenceStore.getLong("default_chapter_sort_by_ascending_or_descending", Anime.EPISODE_SORT_DESC)

    fun setEpisodeSettingsDefault(anime: Anime) {
        filterEpisodeBySeen().set(anime.unseenFilterRaw)
        filterEpisodeByDownloaded().set(anime.downloadedFilterRaw)
        filterEpisodeByBookmarked().set(anime.bookmarkedFilterRaw)
        // AM (FM) -->
        filterEpisodeByFillermarked().set(anime.fillermarkedFilterRaw)
        // <-- AM (FM)
        sortEpisodeBySourceOrNumber().set(anime.sorting)
        displayEpisodeByNameOrNumber().set(anime.displayMode)
        sortEpisodeByAscendingOrDescending().set(if (anime.sortDescending()) Anime.EPISODE_SORT_DESC else Anime.EPISODE_SORT_ASC)
    }

    // AM (GU) -->
    fun groupLibraryBy() = preferenceStore.getInt("group_library_by", LibraryGroup.BY_DEFAULT)
    // <-- AM (GU)

    companion object {
        const val DEVICE_ONLY_ON_WIFI = "wifi"
        const val DEVICE_NETWORK_NOT_METERED = "network_not_metered"
        const val DEVICE_CHARGING = "ac"
        const val DEVICE_BATTERY_NOT_LOW = "battery_not_low"

        const val ENTRY_NON_COMPLETED = "anime_ongoing"
        const val ENTRY_HAS_UNVIEWED = "anime_fully_seen"
        const val ENTRY_NON_VIEWED = "anime_started"
    }
}
