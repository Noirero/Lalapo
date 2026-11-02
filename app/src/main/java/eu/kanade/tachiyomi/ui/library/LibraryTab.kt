package eu.kanade.tachiyomi.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.util.fastAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import animiru.domain.player.service.PlayerPreferences
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.anime.components.LibraryBottomActionMenu
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.library.DeleteLibraryAnimeDialog
import eu.kanade.presentation.library.LibrarySettingsDialog
import eu.kanade.presentation.library.components.LibraryContent
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.library.components.LibraryToolbarTitle
import eu.kanade.presentation.library.components.modernLibraryBackground
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connection.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connection.discord.DiscordScreen
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.feature.migration.config.MigrationConfigScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.library.model.LibraryAnime
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.i18n.MR
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.isLocal

data object LibraryTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_leave)
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_library),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        requestOpenSettingsSheet()
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val viewModel = metroViewModel<LibraryViewModel>()
        val settingsViewModel = metroViewModel<LibrarySettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val sets by viewModel.categorySets.collectAsStateWithLifecycle()
        val activeSetKey by viewModel.activeSetKey.collectAsStateWithLifecycle()
        val pagerState = rememberPagerState(
            initialPage = state.coercedActiveCategoryIndex,
        ) { state.displayedCategories.size }

        val snackbarHostState = remember { SnackbarHostState() }

        // AM (GROUPING) -->
        val onClickRefresh: (Category?) -> Boolean = { category ->
            val started = LibraryUpdateJob.startNow(
                context = context,
                category = if (state.groupType == LibraryGroup.BY_DEFAULT) category else null,
                group = state.groupType,
                groupExtra = when (state.groupType) {
                    LibraryGroup.BY_DEFAULT -> null
                    LibraryGroup.BY_SOURCE, LibraryGroup.BY_TRACK_STATUS -> category?.id?.toString()
                    LibraryGroup.BY_STATUS -> category?.id?.minus(1)?.toString()
                    else -> null
                },
            )

            scope.launch {
                val msgRes = when {
                    !started -> MR.strings.update_already_running
                    category != null -> MR.strings.updating_category
                    else -> MR.strings.updating_library
                }
                snackbarHostState.showSnackbar(context.stringResource(msgRes))
            }
            started
        }
        // <-- AM (GROUPING)

        // AY -->
        suspend fun openEpisode(episode: Episode) {
            val playerPreferences: PlayerPreferences = context.appGraph.playerPreferences
            val extPlayer = playerPreferences.alwaysUseExternalPlayer.get()
            MainActivity.startPlayerActivity(context, episode.animeId, episode.id, extPlayer)
        }
        // <-- AY

        fun openRandomAnime() {
            scope.launch {
                val randomItem = viewModel.getRandomLibraryItemForCurrentCategory()
                if (randomItem != null) {
                    navigator.push(AnimeScreen(randomItem.libraryAnime.anime.id))
                } else {
                    snackbarHostState.showSnackbar(
                        context.stringResource(MR.strings.information_no_entries_found),
                    )
                }
            }
        }

        val isModern = LocalModernVisualPalette.current.isModern

        Scaffold(
            modifier = Modifier.modernLibraryBackground(),
            containerColor = if (isModern) Color.Transparent else MaterialTheme.colorScheme.background,
            topBar = { scrollBehavior ->
                val title = LibraryToolbarTitle(
                    text = stringResource(MR.strings.label_library),
                    numberOfAnime = state.libraryData.favorites.size.takeIf { state.showAnimeCount },
                )
                LibraryToolbar(
                    hasActiveFilters = state.hasActiveFilters,
                    selectedCount = state.selection.size,
                    title = title,
                    sets = sets,
                    activeSetKey = activeSetKey,
                    categories = state.displayedCategories,
                    activeCategoryIndex = pagerState.currentPage,
                    showCategoryTabs = state.showCategoryTabs,
                    groupType = state.groupType,
                    hasCategories = state.hasCategories,
                    getItemCountForCategory = state::getItemCountForCategory,
                    onClickSettings = {
                        navigator.push(SettingsScreen())
                    },
                    onClickUnselectAll = viewModel::clearSelection,
                    onClickSelectAll = viewModel::selectAll,
                    onClickInvertSelection = viewModel::invertSelection,
                    onSelectSet = viewModel::selectSet,
                    onManageSets = { navigator.push(CategoryScreen()) },
                    onSelectCategory = { index ->
                        if (index in state.displayedCategories.indices) {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    },
                    onClickFilter = {
                        viewModel.showSettingsDialog(LibraryViewModel.SettingsPage.Filter)
                    },
                    onClickSort = {
                        viewModel.showSettingsDialog(LibraryViewModel.SettingsPage.Sort)
                    },
                    onClickRefresh = { onClickRefresh(state.activeCategory) },
                    onClickGlobalUpdate = { onClickRefresh(null) },
                    onClickOpenRandomAnime = ::openRandomAnime,
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::search,
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                LibraryBottomActionMenu(
                    visible = state.selectionMode,
                    onChangeCategoryClicked = viewModel::openChangeCategoryDialog,
                    onMarkAsSeenClicked = { viewModel.markSeenSelection(true) },
                    onMarkAsUnseenClicked = { viewModel.markSeenSelection(false) },
                    onDownloadClicked = viewModel::performDownloadAction
                        .takeIf { state.selectedAnime.fastAll { !it.isLocal() } },
                    onDeleteClicked = viewModel::openDeleteAnimeDialog,
                    onMigrateClicked = {
                        val selection = state.selection
                        viewModel.clearSelection()
                        navigator.push(MigrationConfigScreen(selection))
                    },
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { contentPadding ->
            when {
                state.isLoading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }
                state.searchQuery.isNullOrEmpty() && !state.hasActiveFilters && state.isLibraryEmpty -> {
                    EmptyScreen(
                        stringRes = MR.strings.information_empty_library,
                        modifier = Modifier.padding(contentPadding),
                        actions = listOf(
                            EmptyScreenAction(
                                stringRes = AMMR.strings.am_label_discover,
                                icon = Icons.Outlined.Explore,
                                onClick = {
                                    scope.launch {
                                        HomeScreen.openTab(HomeScreen.Tab.Browse())
                                    }
                                },
                            ),
                        ),
                    )
                }
                else -> {
                    LibraryContent(
                        categories = state.displayedCategories,
                        pagerState = pagerState,
                        searchQuery = state.searchQuery,
                        selection = state.selection,
                        contentPadding = contentPadding,
                        hasActiveFilters = state.hasActiveFilters,
                        onChangeCurrentPage = viewModel::updateActiveCategoryIndex,
                        onClickAnime = { navigator.push(AnimeScreen(it)) },
                        continueWatchingItems = state.continueWatchingItems,
                        onContinueWatchingClicked = { it: LibraryAnime ->
                            scope.launchIO {
                                val episode = viewModel.getNextUnseenEpisode(it.anime)
                                // AY -->
                                if (episode != null) openEpisode(episode)
                                // <-- AY
                            }
                            Unit
                        }.takeIf { state.showAnimeContinueButton },
                        onToggleSelection = viewModel::toggleSelection,
                        onToggleRangeSelection = { category, anime ->
                            viewModel.toggleRangeSelection(category, anime)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onRefresh = { onClickRefresh(state.activeCategory) },
                        onGlobalSearchClicked = {
                            navigator.push(GlobalSearchScreen(viewModel.state.value.searchQuery ?: ""))
                        },
                        getDisplayMode = { viewModel.getDisplayMode() },
                        getColumnsForOrientation = { viewModel.getColumnsForOrientation(it) },
                        getItemsForCategory = { state.getItemsForCategory(it) },
                    )
                }
            }
        }

        val onDismissRequest = viewModel::closeDialog
        when (val dialog = state.dialog) {
            is LibraryViewModel.Dialog.SettingsSheet -> run {
                LibrarySettingsDialog(
                    onDismissRequest = onDismissRequest,
                    viewModel = settingsViewModel,
                    category = state.activeCategory,
                    initialPage = dialog.initialPage,
                    // AM (GROUPING) -->
                    hasCategories = state.hasCategories,
                    // <-- AM (GROUPING)
                )
            }
            is LibraryViewModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = {
                        viewModel.clearSelection()
                        navigator.push(CategoryScreen())
                    },
                    onConfirm = { include, exclude ->
                        viewModel.clearSelection()
                        viewModel.setAnimeCategories(dialog.anime, include, exclude)
                    },
                )
            }
            is LibraryViewModel.Dialog.DeleteAnime -> {
                DeleteLibraryAnimeDialog(
                    containsLocalAnime = dialog.anime.any(Anime::isLocal),
                    onDismissRequest = onDismissRequest,
                    onConfirm = { deleteAnime, deleteEpisode ->
                        viewModel.removeAnimes(dialog.anime, deleteAnime, deleteEpisode)
                        viewModel.clearSelection()
                    },
                )
            }
            null -> {}
        }

        LaunchedEffect(state.libraryData.isInitialized) {
            if (state.libraryData.isInitialized && state.displayedCategories.isNotEmpty()) {
                pagerState.scrollToPage(state.coercedActiveCategoryIndex)
            }
        }

        BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
            when {
                state.selectionMode -> viewModel.clearSelection()
                state.searchQuery != null -> viewModel.search(null)
            }
        }

        LaunchedEffect(state.selectionMode, state.dialog) {
            HomeScreen.showBottomNav(!state.selectionMode)
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }

        LaunchedEffect(Unit) {
            // AM (DISCORD_RPC) -->
            with(DiscordRPCService) {
                discordScope.launchIO { setScreen(context.applicationContext, DiscordScreen.LIBRARY) }
            }
            // <-- AM (DISCORD_RPC)
            launch { queryEvent.receiveAsFlow().collect(viewModel::search) }
            launch {
                requestSettingsSheetEvent.receiveAsFlow().collectLatest {
                    viewModel.showSettingsDialog(LibraryViewModel.SettingsPage.Display)
                }
            }
        }
    }

    // For invoking search from other screen
    private val queryEvent = Channel<String>()
    suspend fun search(query: String) = queryEvent.send(query)

    // For opening settings sheet in LibraryController
    private val requestSettingsSheetEvent = Channel<Unit>()
    private suspend fun requestOpenSettingsSheet() = requestSettingsSheetEvent.send(Unit)
}
