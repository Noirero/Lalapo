package eu.kanade.tachiyomi.data.preference

/**
 * This class stores the keys for the preferences in the application.
 */
object PreferenceKeys {

    const val confirmExit = "pref_confirm_exit"

    const val preserveWatchingPosition = "pref_preserve_watching_position"

    const val pipEpisodeToasts = "pref_pip_episode_toasts"

    const val pipOnExit = "pref_pip_on_exit"

    const val mpvConf = "pref_mpv_conf"
    const val defaultPlayerOrientationType = "pref_default_player_orientation_type_key"

    const val adjustOrientationVideoDimensions = "pref_adjust_orientation_video_dimensions"

    const val defaultPlayerOrientationLandscape = "pref_default_player_orientation_landscape_key"

    const val defaultPlayerOrientationPortrait = "pref_default_player_orientation_portrait_key"

    const val playerSpeed = "pref_player_speed"

    const val playerSmoothSeek = "pref_player_smooth_seek"

    const val playerViewMode = "pref_player_view_mode"

    const val progressPreference = "pref_progress_preference"

    const val defaultIntroLength = "pref_default_intro_length"

    const val skipLengthPreference = "pref_skip_length_preference"

    const val alwaysUseExternalPlayer = "pref_always_use_external_player"

    const val externalPlayerPreference = "external_player_preference"

    const val autoUpdateTrack = "pref_auto_update_manga_sync_key"

    const val useExternalDownloader = "use_external_downloader"

    const val externalDownloaderSelection = "external_downloader_selection"

    const val downloadOnlyOverWifi = "pref_download_only_over_wifi_key"

    const val removeAfterSeenSlots = "remove_after_seen_slots"

    const val downloadAfterSeenSlots = "download_after_seen_slots"

    const val trackOnAddingToLibrary = "track_on_adding_to_library"

    const val downloadOnAddingToLibrary = "download_on_adding_to_library"

    const val removeAfterMarkedAsSeen = "pref_remove_after_marked_as_seen_key"

    const val removeBookmarkedChapters = "pref_remove_bookmarked"

    const val filterDownloaded = "pref_filter_library_downloaded"

    const val filterUnread = "pref_filter_library_unread"

    const val filterStarted = "pref_filter_library_started"

    const val filterCompleted = "pref_filter_library_completed"

    const val filterTracked = "pref_filter_library_tracked"

    const val librarySortingMode = "library_sorting_mode"
    const val librarySortingDirection = "library_sorting_ascending"

    const val migrationSortingMode = "pref_migration_sorting"
    const val migrationSortingDirection = "pref_migration_direction"

    const val enableDiscordRPC = "pref_enable_discord_rpc"

    const val startScreen = "start_screen"

    const val hideNotificationContent = "hide_notification_content"

    const val autoUpdateMetadata = "auto_update_metadata"

    const val autoUpdateTrackers = "auto_update_trackers"

    const val downloadNewEpisode = "download_new_episode"

    const val dateFormat = "app_date_format"

    const val defaultAnimeCategory = "default_anime_category"

    const val skipRead = "skip_read"

    const val skipFiltered = "skip_filtered"

    const val searchPinnedSourcesOnly = "search_pinned_sources_only"

    const val dohProvider = "doh_provider"

    const val defaultEpisodeFilterBySeen = "default_episode_filter_by_seen"

    const val defaultEpisodeFilterByDownloaded = "default_episode_filter_by_downloaded"

    const val defaultEpisodeFilterByBookmarked = "default_episode_filter_by_bookmarked"

    const val defaultEpisodeFilterByFillermarked = "default_episode_filter_by_fillermarked"

    const val defaultEpisodeSortBySourceOrNumber = "default_episode_sort_by_source_or_number" // and upload date

    const val defaultEpisodeSortByAscendingOrDescending = "default_episode_sort_by_ascending_or_descending"

    const val defaultEpisodeDisplayByNameOrNumber = "default_episode_display_by_name_or_number"

    const val verboseLogging = "verbose_logging"

    const val autoClearChapterCache = "auto_clear_chapter_cache"

    fun trackUsername(syncId: Long) = "pref_mangasync_username_$syncId"

    fun trackPassword(syncId: Long) = "pref_mangasync_password_$syncId"

    fun trackToken(syncId: Long) = "track_token_$syncId"
}
