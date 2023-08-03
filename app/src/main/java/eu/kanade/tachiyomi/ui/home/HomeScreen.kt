package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.AnimeDownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
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

    private val uiPreferences: UiPreferences by injectLazy()
    private val playerPreferences: PlayerPreferences by injectLazy()

    // AM (UH) -->
    val tabsYUYH = listOf(
        AnimeLibraryTab,
        UpdatesTab(
            fromMore = false,
            externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
        ),
        HistoryTab(
            fromMore = false,
            externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
        ),
        // AM (BROWSE) -->
        BrowseTab,
        // <-- AM (BROWSE)
        MoreTab,
    )

    val tabsYUNH = listOf(
        AnimeLibraryTab,
        UpdatesTab(
            fromMore = false,
            externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
        ),
        // AM (BROWSE) -->
        BrowseTab,
        // <-- AM (BROWSE)
        MoreTab,
    )

    val tabsNUYH = listOf(
        AnimeLibraryTab,
        HistoryTab(
            fromMore = false,
            externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
        ),
        // AM (BROWSE) -->
        BrowseTab,
        // <-- AM (BROWSE)
        MoreTab,
    )

    val tabsNUNH = listOf(
        AnimeLibraryTab,
        // AM (BROWSE) -->
        BrowseTab,
        // <-- AM (BROWSE)
        MoreTab,
    )
    // <-- AM (UH)

    var tabs = if (uiPreferences.showNavUpdates().get()) {
        if (uiPreferences.showNavHistory().get()) {
            tabsYUYH
        } else {
            tabsYUNH
        }
    } else {
        if (uiPreferences.showNavHistory().get()) {
            tabsNUYH
        } else {
            tabsNUNH
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val defaultTab = AnimeLibraryTab
        TabNavigator(
            tab = defaultTab,
        ) { tabNavigator ->
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    // AM (NAVPILL) -->
                    bottomBar = {
                        val bottomNavVisible by produceState(initialValue = true) {
                            showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                        }
                        AnimatedVisibility(
                            visible = bottomNavVisible,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            NavigationPill(tabs = tabs)
                        }
                    },
                    overlayBottomBar = true,
                    // <-- AM (NAVPILL)
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
                                materialFadeThroughIn(initialScale = 1f, durationMillis = TabFadeDuration) with
                                    materialFadeThroughOut(durationMillis = TabFadeDuration)
                            },
                            content = {
                                tabNavigator.saveableState(key = "currentTab", it) {
                                    it.Content()
                                }
                            },
                        )
                    }
                }
            }

            val goToAnimelibTab = { tabNavigator.current = AnimeLibraryTab }
            BackHandler(
                enabled = tabNavigator.current != AnimeLibraryTab,
                onBack = goToAnimelibTab,
            )

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        goToAnimelibTab()
                        AnimeLibraryTab.search(it)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Animelib -> {
                                AnimeLibraryTab
                            }
                            is Tab.Updates -> {
                                UpdatesTab(
                                    fromMore = !uiPreferences.showNavUpdates().get(),
                                    externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
                                )
                            }
                            is Tab.History -> {
                                HistoryTab(
                                    fromMore = !uiPreferences.showNavHistory().get(),
                                    externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
                                )
                            }
                            is Tab.Browse -> {
                                BrowseTab
                            }
                            is Tab.More -> {
                                MoreTab
                            }
                        }
                        if (it is Tab.More && it.toDownloads) {
                            navigator.push(AnimeDownloadQueueScreen)
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

    sealed class Tab {
        data class Animelib(val animeIdToOpen: Long? = null) : Tab()
        object Updates : Tab()
        object History : Tab()
        object Browse : Tab()
        data class More(val toDownloads: Boolean) : Tab()
    }
}
