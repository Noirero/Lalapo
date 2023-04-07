package eu.kanade.tachiyomi.ui.library.anime

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.category.anime.interactor.GetAnimeCategories
import eu.kanade.domain.category.anime.interactor.SetDisplayModeForAnimeCategory
import eu.kanade.domain.category.anime.interactor.SetSortModeForAnimeCategory
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.library.model.LibraryDisplayMode
import eu.kanade.domain.library.model.LibraryGroup
import eu.kanade.domain.library.model.LibrarySort
import eu.kanade.domain.library.model.display
import eu.kanade.domain.library.model.sort
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.AnimeTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.State
import eu.kanade.tachiyomi.widget.sheet.TabbedBottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class AnimeLibrarySettingsSheet(
    activity: Activity,
    private val trackManager: TrackManager = Injekt.get(),
    private val setDisplayModeForCategory: SetDisplayModeForAnimeCategory = Injekt.get(),
    private val setSortModeForCategory: SetSortModeForAnimeCategory = Injekt.get(),
) : TabbedBottomSheetDialog(activity) {

    val filters: Filter
    private val sort: Sort
    private val display: Display

    // AM (GU) -->
    private val grouping: Grouping
    // <-- AM (GU)

    val sheetScope = CoroutineScope(Job() + Dispatchers.IO)

    init {
        filters = Filter(activity)
        sort = Sort(activity)
        display = Display(activity)
        // AM (GU) -->
        grouping = Grouping(activity)
        // <-- AM (GU)
    }

    /**
     * adjusts selected button to match real state.
     * @param currentCategory ID of currently shown category
     */
    fun show(currentCategory: Category) {
        sort.currentCategory = currentCategory
        sort.adjustDisplaySelection()
        display.currentCategory = currentCategory
        display.adjustDisplaySelection()
        super.show()
    }

    override fun getTabViews(): List<View> = listOf(
        filters,
        sort,
        display,
        // AM (GU) -->
        grouping,
        // <-- AM (GU)
    )

    override fun getTabTitles(): List<Int> = listOf(
        R.string.action_filter,
        R.string.action_sort,
        R.string.action_display,
        // AM (GU) -->
        R.string.group,
        // <-- AM (GU)
    )

    /**
     * Filters group (unseen, downloaded, ...).
     */
    inner class Filter @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val filterGroup = FilterGroup()

        init {
            setGroups(listOf(filterGroup))
        }

        /**
         * Returns true if there's at least one filter from [FilterGroup] active.
         */
        fun hasActiveFilters(): Boolean {
            return filterGroup.items.filterIsInstance<Item.TriStateGroup>().any { it.state != State.IGNORE.value }
        }

        inner class FilterGroup : Group {

            private val downloaded = Item.TriStateGroup(R.string.label_downloaded, this)
            private val unseen = Item.TriStateGroup(R.string.action_filter_unseen, this)
            private val started = Item.TriStateGroup(R.string.label_started, this)
            private val bookmarked = Item.TriStateGroup(R.string.action_filter_bookmarked, this)

            // AM (FM) -->
            private val fillermarked = Item.TriStateGroup(R.string.action_filter_fillermarked, this)

            // <-- AM (FM)
            private val completed = Item.TriStateGroup(R.string.completed, this)
            private val trackFilters: Map<Long, Item.TriStateGroup>

            override val header = null
            override val items: List<Item>
            override val footer = null

            init {
                trackManager.services.filter { service -> service.isLogged && service is AnimeTrackService }
                    .also { services ->
                        val size = services.size
                        trackFilters = services.associate { service ->
                            Pair(service.id, Item.TriStateGroup(getServiceResId(service, size), this))
                        }
                        // AM (FM)>
                        val list: MutableList<Item> = mutableListOf(downloaded, unseen, started, bookmarked, fillermarked, completed)
                        if (size > 1) list.add(Item.Header(R.string.action_filter_tracked))
                        list.addAll(trackFilters.values)
                        items = list
                    }
            }

            private fun getServiceResId(service: TrackService, size: Int): Int {
                return if (size > 1) service.nameRes() else R.string.action_filter_tracked
            }

            override fun initModels() {
                if (preferences.downloadedOnly().get()) {
                    downloaded.state = State.INCLUDE.value
                    downloaded.enabled = false
                } else {
                    downloaded.state = libraryPreferences.filterDownloadedAnime().get()
                }
                unseen.state = libraryPreferences.filterUnseen().get()
                started.state = libraryPreferences.filterStartedAnime().get()
                bookmarked.state = libraryPreferences.filterBookmarkedAnime().get()
                // AM (FM) -->
                fillermarked.state = libraryPreferences.filterFillermarkedAnime().get()
                // <-- AM (FM)
                completed.state = libraryPreferences.filterCompletedAnime().get()

                trackFilters.forEach { trackFilter ->
                    trackFilter.value.state = libraryPreferences.filterTrackedAnime(trackFilter.key.toInt()).get()
                }
            }

            override fun onItemClicked(item: Item) {
                item as Item.TriStateGroup
                val newState = when (item.state) {
                    State.IGNORE.value -> State.INCLUDE.value
                    State.INCLUDE.value -> State.EXCLUDE.value
                    State.EXCLUDE.value -> State.IGNORE.value
                    else -> throw Exception("Unknown State")
                }
                item.state = newState
                when (item) {
                    downloaded -> libraryPreferences.filterDownloadedAnime().set(newState)
                    unseen -> libraryPreferences.filterUnseen().set(newState)
                    started -> libraryPreferences.filterStartedAnime().set(newState)
                    bookmarked -> libraryPreferences.filterBookmarkedAnime().set(newState)
                    // AM (FM) -->
                    fillermarked -> libraryPreferences.filterFillermarkedAnime().set(newState)
                    // <-- AM (FM)
                    completed -> libraryPreferences.filterCompletedAnime().set(newState)
                    else -> {
                        trackFilters.forEach { trackFilter ->
                            if (trackFilter.value == item) {
                                libraryPreferences.filterTrackedAnime(trackFilter.key.toInt()).set(newState)
                            }
                        }
                    }
                }

                adapter.notifyItemChanged(item)
            }
        }
    }

    /**
     * Sorting group (alphabetically, by last seen, ...) and ascending or descending.
     */
    inner class Sort @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val sort = SortGroup()

        init {
            setGroups(listOf(sort))
        }

        // Refreshes Display Setting selections
        fun adjustDisplaySelection() {
            sort.initModels()
            sort.items.forEach { adapter.notifyItemChanged(it) }
        }

        inner class SortGroup : Group {

            private val alphabetically = Item.MultiSort(R.string.action_sort_alpha, this)
            private val total = Item.MultiSort(R.string.action_sort_total_episodes, this)
            private val lastSeen = Item.MultiSort(R.string.action_sort_last_seen, this)
            private val lastChecked = Item.MultiSort(R.string.action_sort_last_anime_update, this)
            private val unseen = Item.MultiSort(R.string.action_sort_unseen_count, this)
            private val latestEpisode = Item.MultiSort(R.string.action_sort_latest_episode, this)
            private val episodeFetchDate = Item.MultiSort(R.string.action_sort_episode_fetch_date, this)
            private val dateAdded = Item.MultiSort(R.string.action_sort_date_added, this)

            override val header = null
            override val items =
                listOf(alphabetically, lastSeen, lastChecked, unseen, total, latestEpisode, episodeFetchDate, dateAdded)
            override val footer = null

            override fun initModels() {
                // AM (GU) -->
                val sort = if (libraryPreferences.groupLibraryBy().get() == LibraryGroup.BY_DEFAULT) {
                    currentCategory?.sort ?: LibrarySort.default
                } else {
                    libraryPreferences.librarySortingMode().get()
                }
                // <-- AM (GU)
                val order = if (sort.isAscending) Item.MultiSort.SORT_ASC else Item.MultiSort.SORT_DESC

                alphabetically.state =
                    if (sort.type == LibrarySort.Type.Alphabetical) order else Item.MultiSort.SORT_NONE
                lastSeen.state =
                    if (sort.type == LibrarySort.Type.LastRead) order else Item.MultiSort.SORT_NONE
                lastChecked.state =
                    if (sort.type == LibrarySort.Type.LastUpdate) order else Item.MultiSort.SORT_NONE
                unseen.state =
                    if (sort.type == LibrarySort.Type.UnreadCount) order else Item.MultiSort.SORT_NONE
                total.state =
                    if (sort.type == LibrarySort.Type.TotalChapters) order else Item.MultiSort.SORT_NONE
                latestEpisode.state =
                    if (sort.type == LibrarySort.Type.LatestChapter) order else Item.MultiSort.SORT_NONE
                episodeFetchDate.state =
                    if (sort.type == LibrarySort.Type.ChapterFetchDate) order else Item.MultiSort.SORT_NONE
                dateAdded.state =
                    if (sort.type == LibrarySort.Type.DateAdded) order else Item.MultiSort.SORT_NONE
            }

            override fun onItemClicked(item: Item) {
                item as Item.MultiStateGroup
                val prevState = item.state

                item.group.items.forEach {
                    (it as Item.MultiStateGroup).state =
                        Item.MultiSort.SORT_NONE
                }
                item.state = when (prevState) {
                    Item.MultiSort.SORT_NONE -> Item.MultiSort.SORT_ASC
                    Item.MultiSort.SORT_ASC -> Item.MultiSort.SORT_DESC
                    Item.MultiSort.SORT_DESC -> Item.MultiSort.SORT_ASC
                    else -> throw Exception("Unknown state")
                }

                setSortPreference(item)

                item.group.items.forEach { adapter.notifyItemChanged(it) }
            }

            private fun setSortPreference(item: Item.MultiStateGroup) {
                val mode = when (item) {
                    alphabetically -> LibrarySort.Type.Alphabetical
                    lastSeen -> LibrarySort.Type.LastRead
                    lastChecked -> LibrarySort.Type.LastUpdate
                    unseen -> LibrarySort.Type.UnreadCount
                    total -> LibrarySort.Type.TotalChapters
                    latestEpisode -> LibrarySort.Type.LatestChapter
                    episodeFetchDate -> LibrarySort.Type.ChapterFetchDate
                    dateAdded -> LibrarySort.Type.DateAdded
                    else -> throw NotImplementedError("Unknown display mode")
                }
                val direction = if (item.state == Item.MultiSort.SORT_ASC) {
                    LibrarySort.Direction.Ascending
                } else {
                    LibrarySort.Direction.Descending
                }

                sheetScope.launchIO {
                    setSortModeForCategory.await(currentCategory!!, mode, direction)
                }
            }
        }
    }

    /**
     * Display group, to show the library as a list or a grid.
     */
    inner class Display @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val displayGroup: DisplayGroup
        private val badgeGroup: BadgeGroup
        private val tabsGroup: TabsGroup
        private val otherGroup: OtherGroup

        init {
            displayGroup = DisplayGroup()
            badgeGroup = BadgeGroup()
            tabsGroup = TabsGroup()
            otherGroup = OtherGroup()
            setGroups(listOf(displayGroup, badgeGroup, tabsGroup, otherGroup))
        }

        // Refreshes Display Setting selections
        fun adjustDisplaySelection() {
            val mode = getDisplayModePreference()
            displayGroup.setGroupSelections(mode)
            displayGroup.items.forEach { adapter.notifyItemChanged(it) }
        }

        // Gets user preference of currently selected display mode at current category
        private fun getDisplayModePreference(): LibraryDisplayMode {
            // AM (GU) -->
            return if (libraryPreferences.groupLibraryBy().get() == LibraryGroup.BY_DEFAULT) {
                currentCategory?.display ?: LibraryDisplayMode.default
            } else {
                libraryPreferences.libraryDisplayMode().get()
            }
            // <-- AM (GU)
        }

        inner class DisplayGroup : Group {

            private val compactGrid = Item.Radio(R.string.action_display_grid, this)
            private val comfortableGrid = Item.Radio(R.string.action_display_comfortable_grid, this)
            private val coverOnlyGrid = Item.Radio(R.string.action_display_cover_only_grid, this)
            private val list = Item.Radio(R.string.action_display_list, this)

            override val header = Item.Header(R.string.action_display_mode)
            override val items = listOf(compactGrid, comfortableGrid, coverOnlyGrid, list)
            override val footer = null

            override fun initModels() {
                val mode = getDisplayModePreference()
                setGroupSelections(mode)
            }

            override fun onItemClicked(item: Item) {
                item as Item.Radio
                if (item.checked) return

                item.group.items.forEach { (it as Item.Radio).checked = false }
                item.checked = true

                setDisplayModePreference(item)

                item.group.items.forEach { adapter.notifyItemChanged(it) }
            }

            // Sets display group selections based on given mode
            fun setGroupSelections(mode: LibraryDisplayMode) {
                compactGrid.checked = mode == LibraryDisplayMode.CompactGrid
                comfortableGrid.checked = mode == LibraryDisplayMode.ComfortableGrid
                coverOnlyGrid.checked = mode == LibraryDisplayMode.CoverOnlyGrid
                list.checked = mode == LibraryDisplayMode.List
            }

            private fun setDisplayModePreference(item: Item) {
                val flag = when (item) {
                    compactGrid -> LibraryDisplayMode.CompactGrid
                    comfortableGrid -> LibraryDisplayMode.ComfortableGrid
                    coverOnlyGrid -> LibraryDisplayMode.CoverOnlyGrid
                    list -> LibraryDisplayMode.List
                    else -> throw NotImplementedError("Unknown display mode")
                }

                sheetScope.launchIO {
                    setDisplayModeForCategory.await(currentCategory!!, flag)
                }
            }
        }

        inner class BadgeGroup : Group {
            private val downloadBadge = Item.CheckboxGroup(R.string.action_display_download_badge_anime, this)
            private val unseenBadge = Item.CheckboxGroup(R.string.action_display_unseen_badge, this)
            private val localBadge = Item.CheckboxGroup(R.string.action_display_local_badge_anime, this)
            private val languageBadge = Item.CheckboxGroup(R.string.action_display_language_badge, this)

            override val header = Item.Header(R.string.badges_header)
            override val items = listOf(downloadBadge, unseenBadge, localBadge, languageBadge)
            override val footer = null

            override fun initModels() {
                downloadBadge.checked = libraryPreferences.downloadBadge().get()
                unseenBadge.checked = libraryPreferences.unviewedBadge().get()
                localBadge.checked = libraryPreferences.localBadge().get()
                languageBadge.checked = libraryPreferences.languageBadge().get()
            }

            override fun onItemClicked(item: Item) {
                item as Item.CheckboxGroup
                item.checked = !item.checked
                when (item) {
                    downloadBadge -> libraryPreferences.downloadBadge().set((item.checked))
                    unseenBadge -> libraryPreferences.unviewedBadge().set((item.checked))
                    localBadge -> libraryPreferences.localBadge().set((item.checked))
                    languageBadge -> libraryPreferences.languageBadge().set((item.checked))
                    else -> {}
                }
                adapter.notifyItemChanged(item)
            }
        }

        inner class TabsGroup : Group {
            private val showTabs = Item.CheckboxGroup(R.string.action_display_show_tabs, this)
            private val showNumberOfItems = Item.CheckboxGroup(R.string.action_display_show_number_of_items, this)

            override val header = Item.Header(R.string.tabs_header)
            override val items = listOf(showTabs, showNumberOfItems)
            override val footer = null

            override fun initModels() {
                showTabs.checked = libraryPreferences.categoryTabs().get()
                showNumberOfItems.checked = libraryPreferences.categoryNumberOfItems().get()
            }

            override fun onItemClicked(item: Item) {
                item as Item.CheckboxGroup
                item.checked = !item.checked
                when (item) {
                    showTabs -> libraryPreferences.categoryTabs().set(item.checked)
                    showNumberOfItems -> libraryPreferences.categoryNumberOfItems().set(item.checked)
                    else -> {}
                }
                adapter.notifyItemChanged(item)
            }
        }

        inner class OtherGroup : Group {
            private val showContinueWatchingButton = Item.CheckboxGroup(R.string.action_display_show_continue_reading_button, this)

            override val header = Item.Header(R.string.other_header)
            override val items = listOf(showContinueWatchingButton)
            override val footer = null

            override fun initModels() {
                showContinueWatchingButton.checked = libraryPreferences.showContinueViewingButton().get()
            }

            override fun onItemClicked(item: Item) {
                item as Item.CheckboxGroup
                item.checked = !item.checked
                when (item) {
                    showContinueWatchingButton -> libraryPreferences.showContinueViewingButton().set(item.checked)
                    else -> {}
                }
                adapter.notifyItemChanged(item)
            }
        }
    }

    // AM (GU) -->
    inner class Grouping @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        init {
            setGroups(listOf(InternalGroup()))
        }

        inner class InternalGroup : Group {
            private val groupItems = mutableListOf<Item.DrawableSelection>()
            private val trackManager: TrackManager = Injekt.get()
            private val hasCategories = runBlocking {
                Injekt.get<GetAnimeCategories>().await().filterNot(Category::isSystemCategory).isNotEmpty()
            }

            init {
                val groupingItems = mutableListOf(
                    LibraryGroup.BY_DEFAULT,
                    LibraryGroup.BY_SOURCE,
                    LibraryGroup.BY_STATUS,
                )
                if (trackManager.hasLoggedServices()) {
                    groupingItems.add(LibraryGroup.BY_TRACK_STATUS)
                }
                if (hasCategories) {
                    groupingItems.add(LibraryGroup.UNGROUPED)
                }
                groupItems += groupingItems.map { id ->
                    Item.DrawableSelection(
                        id,
                        this,
                        LibraryGroup.groupTypeStringRes(id, hasCategories),
                        LibraryGroup.groupTypeDrawableRes(id),
                    )
                }
            }

            override val header = null
            override val items = groupItems
            override val footer = null

            override fun initModels() {
                val groupType = libraryPreferences.groupLibraryBy().get()

                items.forEach {
                    it.state = if (it.id == groupType) {
                        Item.DrawableSelection.SELECTED
                    } else {
                        Item.DrawableSelection.NOT_SELECTED
                    }
                }
            }

            override fun onItemClicked(item: Item) {
                item as Item.DrawableSelection
                item.group.items.forEach {
                    (it as Item.DrawableSelection).state =
                        Item.DrawableSelection.NOT_SELECTED
                }
                item.state = Item.DrawableSelection.SELECTED

                libraryPreferences.groupLibraryBy().set(item.id)

                item.group.items.forEach { adapter.notifyItemChanged(it) }
            }
        }
    }
    // <-- AM (GU)

    open inner class Settings(context: Context, attrs: AttributeSet?) :
        ExtendedNavigationView(context, attrs) {

        val preferences: BasePreferences by injectLazy()
        val libraryPreferences: LibraryPreferences by injectLazy()
        lateinit var adapter: Adapter

        /**
         * Click listener to notify the parent fragment when an item from a group is clicked.
         */
        var onGroupClicked: (Group) -> Unit = {}

        var currentCategory: Category? = null

        fun setGroups(groups: List<Group>) {
            adapter = Adapter(groups.map { it.createItems() }.flatten())
            recycler.adapter = adapter

            groups.forEach { it.initModels() }
            addView(recycler)
        }

        /**
         * Adapter of the recycler view.
         */
        inner class Adapter(items: List<Item>) : ExtendedNavigationView.Adapter(items) {

            override fun onItemClicked(item: Item) {
                if (item is GroupedItem) {
                    item.group.onItemClicked(item)
                    onGroupClicked(item.group)
                }
            }
        }
    }
}
