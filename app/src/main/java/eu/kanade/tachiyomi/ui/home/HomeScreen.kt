package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.NavigationBar
import eu.kanade.presentation.components.NavigationRail
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

object HomeScreen : Screen {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    private const val TabFadeDuration = 300

    private val uiPreferences: UiPreferences by injectLazy()
    private val playerPreferences: PlayerPreferences by injectLazy()

    // AM (BR) -->
    private val sourcePreferences: SourcePreferences by injectLazy()
    // <-- AM (BR)

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
        // AM (BR) -->
        BrowseTab(
            extHasUpdate = sourcePreferences.animeExtensionUpdatesCount().get() == 0,
        ),
        // <-- AM (BR)
        MoreTab,
    )

    val tabsYUNH = listOf(
        AnimeLibraryTab,
        UpdatesTab(
            fromMore = false,
            externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
        ),
        // AM (BR) -->
        BrowseTab(
            extHasUpdate = sourcePreferences.animeExtensionUpdatesCount().get() == 0,
        ),
        // <-- AM (BR)
        MoreTab,
    )

    val tabsNUYH = listOf(
        AnimeLibraryTab,
        HistoryTab(
            fromMore = false,
            externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
        ),
        // AM (BR) -->
        BrowseTab(
            extHasUpdate = sourcePreferences.animeExtensionUpdatesCount().get() == 0,
        ),
        // <-- AM (BR)
        MoreTab,
    )

    val tabsNUNH = listOf(
        AnimeLibraryTab,
        // AM (BR) -->
        BrowseTab(
            extHasUpdate = sourcePreferences.animeExtensionUpdatesCount().get() == 0,
        ),
        // <-- AM (BR)
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
                    startBar = {
                        if (isTabletUi()) {
                            NavigationRail {
                                tabs.fastForEach {
                                    NavigationRailItem(it)
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!isTabletUi()) {
                            val bottomNavVisible by produceState(initialValue = true) {
                                showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                            }
                            AnimatedVisibility(
                                visible = bottomNavVisible,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                NavigationBar {
                                    tabs.fastForEach {
                                        NavigationBarItem(it)
                                    }
                                }
                            }
                        }
                    },
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
                        // AM (DC) -->
                        DiscordRPCService.setDiscordPage(0)
                        // <-- AM (DC)
                        goToAnimelibTab()
                        AnimeLibraryTab.search(it)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Animelib -> {
                                // AM (DC) -->
                                DiscordRPCService.setDiscordPage(0)
                                // <-- AM (DC)
                                AnimeLibraryTab
                            }
                            is Tab.Updates -> {
                                // AM (DC) -->
                                DiscordRPCService.setDiscordPage(1)
                                // <-- AM (DC)
                                UpdatesTab(
                                    fromMore = !uiPreferences.showNavUpdates().get(),
                                    externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
                                )
                            }
                            is Tab.History -> {
                                // AM (DC) -->
                                DiscordRPCService.setDiscordPage(2)
                                // <-- AM (DC)
                                HistoryTab(
                                    fromMore = !uiPreferences.showNavHistory().get(),
                                    externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
                                )
                            }
                            is Tab.Browse -> {
                                // AM (DC) -->
                                DiscordRPCService.setDiscordPage(3)
                                // <-- AM (DC)
                                BrowseTab(
                                    // AM (BR) -->
                                    extHasUpdate = sourcePreferences.animeExtensionUpdatesCount().get() == 0,
                                    // <-- AM (BR)
                                    toExtensions = it.toExtensions,
                                )
                            }
                            is Tab.More -> {
                                // AM (DC) -->
                                DiscordRPCService.setDiscordPage(4)
                                // <-- AM (DC)
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

    @Composable
    private fun RowScope.NavigationBarItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationBarItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
        )
    }

    @Composable
    fun NavigationRailItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationRailItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
        )
    }

    @Composable
    private fun NavigationIconItem(tab: eu.kanade.presentation.util.Tab) {
        BadgedBox(
            badge = {
                when {
                    UpdatesTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            val pref = Injekt.get<LibraryPreferences>()
                            pref.newAnimeUpdatesCount().changes()
                                .collectLatest { value = if (pref.newShowUpdatesCount().get()) it else 0 }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    id = R.plurals.notification_chapters_generic,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                    BrowseTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            val pref = Injekt.get<SourcePreferences>()
                            pref.animeExtensionUpdatesCount().changes()
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    id = R.plurals.update_check_notification_ext_updates,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                }
            },
        ) {
            Icon(painter = tab.options.icon!!, contentDescription = tab.options.title)
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
        data class Browse(val toExtensions: Boolean = false) : Tab()
        data class More(val toDownloads: Boolean) : Tab()
    }
}
