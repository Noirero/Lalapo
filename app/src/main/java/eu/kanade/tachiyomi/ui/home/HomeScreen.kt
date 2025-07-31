package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import soup.compose.material.motion.animation.materialSharedAxisX
import tachiyomi.presentation.core.components.material.Scaffold
import uy.kohesive.injekt.injectLazy

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    private const val TAB_FADE_DURATION = 300
    private const val TAB_NAVIGATOR_KEY = "HomeTabs"

    private val uiPreferences: UiPreferences by injectLazy()
    private val defaultTab = uiPreferences.startScreen().get().tab

    private val tabs = listOf(
        AnimeLibraryTab,
        // AM (RECENTS) -->
        RecentsTab,
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
            key = TAB_NAVIGATOR_KEY,
        ) { tabNavigator ->
            // AM (NAVIGATION_PILL) -->
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                val currentTabIndex by remember {
                    derivedStateOf { tabs.indexOfFirst { it::class == tabNavigator.current::class } }
                }

                var oldIndex by remember { mutableIntStateOf(currentTabIndex) }

                LaunchedEffect(currentTabIndex) {
                    oldIndex = currentTabIndex
                }

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
                                labelFade = TAB_FADE_DURATION / 2,
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
                                materialSharedAxisX(
                                    forward = oldIndex < currentTabIndex,
                                    slideDistance = 500,
                                    durationMillis = TAB_FADE_DURATION,
                                )
                            },
                            label = "tabContent",
                        ) {
                            tabNavigator.saveableState(key = "currentTab", it) {
                                it.Content()
                            }
                        }
                    }
                    // AM (NAVIGATION_PILL) -->
                    // <-- AM (NAVIGATION_PILL)
                }
            }

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        when (defaultTab) {
                            AnimeLibraryTab -> AnimeLibraryTab.search(it)
                        }
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.AnimeLib -> AnimeLibraryTab
                            // AM (RECENTS) -->
                            is Tab.Recents -> {
                                if (it.toHistory) {
                                    RecentsTab.showHistory()
                                }
                                RecentsTab
                            }
                            // <-- AM (RECENTS)
                            // AM (BROWSE) -->
                            is Tab.Browse -> BrowseTab
                            // <-- AM (BROWSE)
                            is Tab.More -> MoreTab
                        }

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
