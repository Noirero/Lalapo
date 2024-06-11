package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.recents.RecentsTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.presentation.core.components.material.Scaffold
import uy.kohesive.injekt.injectLazy

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    private const val TabFadeDuration = 500
    private const val TabNavigatorKey = "HomeTabs"

    private val uiPreferences: UiPreferences by injectLazy()
    private val defaultTab = uiPreferences.startScreen().get().tab

    private val tabs = listOf(
        AnimeLibraryTab,
        // AM (RECENTS) -->
        RecentsTab(toHistory = false),
        // <-- AM (RECENTS)
        // AM (BROWSE) -->
        BrowseTab,
        // <-- AM (BROWSE)
        MoreTab,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        TabNavigator(
            tab = defaultTab,
            key = TabNavigatorKey,
        ) { tabNavigator ->
            // AM (NAVIGATION_PILL) -->
            val currTabIndex = tabNavigator.current.options.index.toInt()
            var currentTabIndex by remember { mutableIntStateOf(currTabIndex) }
            val setCurrentTabIndex: (Int) -> Unit = { currentTabIndex = it }
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    bottomBar = {
                        val bottomNavVisible by produceState(initialValue = true) {
                            showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                        }
                        AnimatedVisibility(
                            visible = bottomNavVisible,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            NavigationPill(
                                tabs = tabs,
                                currentTabIndex,
                                setCurrentTabIndex,
                                labelFade = TabFadeDuration / 2,
                            )
                        }
                    },
                    // <-- AM (NAVIGATION_PILL)
                    contentWindowInsets = WindowInsets(0),
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .consumeWindowInsets(contentPadding),
                    ) {
                        AnimatedContent(
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                materialFadeThroughIn(
                                    initialScale = 1f,
                                    durationMillis = TabFadeDuration,
                                ) togetherWith
                                    materialFadeThroughOut(durationMillis = TabFadeDuration)
                            },
                            label = "tabContent",
                        ) {
                            tabNavigator.saveableState(key = "currentTab", it) {
                                it.Content()
                            }
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        when (defaultTab) {
                            AnimeLibraryTab -> AnimeLibraryTab.search(it)
                            else -> {}
                        }
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.AnimeLib -> AnimeLibraryTab
                            // AM (RECENTS) -->
                            is Tab.Recents -> RecentsTab(it.toHistory)
                            // <-- AM (RECENTS)
                            // AM (BROWSE) -->
                            is Tab.Browse -> BrowseTab
                            // <-- AM (BROWSE)
                            is Tab.More -> MoreTab
                        }

                        // AM (NAVIGATION_PILL) -->
                        currentTabIndex = when (it) {
                            is Tab.AnimeLib -> 0
                            // AM (RECENTS) -->
                            is Tab.Recents -> 1
                            // <-- AM (RECENTS)
                            is Tab.Browse -> 2
                            is Tab.More -> 3
                        }
                        // <-- AM (NAVIGATION_PILL)

                        if (it is Tab.AnimeLib && it.animeIdToOpen != null) {
                            navigator.push(AnimeScreen(it.animeIdToOpen))
                        }
                        if (it is Tab.More && it.toDownloads) {
                            // AM (REMOVE_TABBED_SCREENS) -->
                            navigator.push(DownloadQueueScreen)
                            // <-- AM (REMOVE_TABBED_SCREENS)
                        }
                    }
                }
            }
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data class AnimeLib(val animeIdToOpen: Long? = null) : Tab

        // AM (RECENTS) -->
        data class Recents(val toHistory: Boolean) : Tab

        // <-- AM (RECENTS)
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
