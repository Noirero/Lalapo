package eu.kanade.tachiyomi.ui.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.core.view.WindowCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.presentation.theme.LocalModernArtworkIdentity
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernPhoneNavigationGeometry
import eu.kanade.presentation.theme.ModernSharpWallpaper
import eu.kanade.presentation.theme.modernLibraryChromeSurface
import eu.kanade.presentation.theme.modernLibraryNeedsLightStatusBarIcons
import eu.kanade.presentation.theme.modernNavigationContainerColor
import eu.kanade.presentation.theme.resolveModernLibraryChrome
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.recents.RecentsTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    @Suppress("ConstPropertyName")
    private const val TabFadeDuration = 200

    @Suppress("ConstPropertyName")
    private const val TabNavigatorKey = "HomeTabs"

    private val TABS = listOf(
        LibraryTab,
        BrowseTab,
        RecentsTab,
        MoreTab,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        TabNavigator(
            tab = LibraryTab,
            key = TabNavigatorKey,
        ) { tabNavigator ->
            val activity = LocalContext.current as? Activity
            val view = LocalView.current
            val useLightStatusBarIcons = modernLibraryNeedsLightStatusBarIcons(
                LocalModernVisualPalette.current,
                LocalModernArtworkIdentity.current,
            )
            if (tabNavigator.current == LibraryTab && useLightStatusBarIcons && activity != null) {
                // Sharp Modern wallpapers are dark at the status bar even when the Material
                // surface is light. Restore the activity's previous icon mode on exit.
                DisposableEffect(activity.window, view) {
                    val controller = WindowCompat.getInsetsController(activity.window, view)
                    val previous = controller.isAppearanceLightStatusBars
                    controller.isAppearanceLightStatusBars = false
                    onDispose { controller.isAppearanceLightStatusBars = previous }
                }
            }

            // AM (NAVIGATION_PILL) -->
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                val tabletUi = isTabletUi()
                val navigationSuiteType = if (tabletUi) {
                    NavigationSuiteType.NavigationRail
                } else {
                    NavigationSuiteType.NavigationBar
                }
                val navigationSuiteState = rememberNavigationSuiteScaffoldState()
                LaunchedEffect(navigationSuiteState, tabletUi) {
                    if (tabletUi) navigationSuiteState.show()
                    showBottomNavEvent.receiveAsFlow().collectLatest { show ->
                        if (tabletUi || show) {
                            navigationSuiteState.show()
                        } else {
                            navigationSuiteState.hide()
                        }
                    }
                }

                // AM -->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (tabNavigator.current::class == LibraryTab::class) {
                        ModernSharpWallpaper(modifier = Modifier.fillMaxSize())
                    }
                    NavigationSuiteScaffoldLayout(
                        // <-- AM
                        navigationSuiteType = navigationSuiteType,
                        state = navigationSuiteState,
                        navigationSuite = {
                            val modernPalette = LocalModernVisualPalette.current
                            val isModern = modernPalette.isModern
                            val chrome = resolveModernLibraryChrome(modernPalette, LocalModernArtworkIdentity.current)
                            val useMiyorareVisuals = chrome != null
                            val navigationShape = chrome?.navShape ?: RoundedCornerShape(30.dp)
                            val navigationColorScheme = if (useMiyorareVisuals) {
                                MaterialTheme.colorScheme.copy(
                                    primaryContainer = Color.Transparent,
                                    secondaryContainer = Color.Transparent,
                                )
                            } else {
                                MaterialTheme.colorScheme
                            }
                            MaterialTheme(
                                colorScheme = navigationColorScheme,
                                shapes = MaterialTheme.shapes,
                                typography = MaterialTheme.typography,
                            ) {
                                NavigationSuite(
                                    modifier = if (useMiyorareVisuals) {
                                        Modifier
                                            .padding(
                                                horizontal = if (tabletUi) {
                                                    8.dp
                                                } else {
                                                    ModernPhoneNavigationGeometry.HorizontalMargin
                                                },
                                                vertical = if (tabletUi) {
                                                    8.dp
                                                } else {
                                                    ModernPhoneNavigationGeometry.VerticalMargin
                                                },
                                            )
                                            .then(
                                                if (tabletUi) {
                                                    Modifier
                                                } else {
                                                    Modifier.heightIn(min = ModernPhoneNavigationGeometry.MinHeight)
                                                },
                                            )
                                            .modernLibraryChromeSurface(chrome!!, shape = navigationShape)
                                    } else {
                                        Modifier
                                    },
                                    navigationSuiteType = navigationSuiteType,
                                    colors = modernNavigationContainerColor(
                                        scheme = MaterialTheme.colorScheme,
                                        isModern = isModern,
                                        useGlass = useMiyorareVisuals,
                                    )?.let { modernContainer ->
                                        NavigationSuiteDefaults.colors(
                                            navigationBarContainerColor = modernContainer,
                                            navigationRailContainerColor = modernContainer,
                                        )
                                    } ?: NavigationSuiteDefaults.colors(
                                        navigationRailContainerColor = MaterialTheme.colorScheme
                                            .surfaceColorAtElevation(3.dp),
                                    ),
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    TABS.fastForEach { NavigationSuiteItem(it, navigationSuiteType) }
                                }
                            }
                        },
                    ) {
                        AnimatedContent(
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                materialFadeThroughIn(
                                    initialScale = 1f,
                                    durationMillis = TabFadeDuration,
                                ) togetherWith materialFadeThroughOut(durationMillis = TabFadeDuration)
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

            val goToLibraryTab = { tabNavigator.current = LibraryTab }

            BackHandler(enabled = tabNavigator.current != LibraryTab, onBack = goToLibraryTab)

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        goToLibraryTab()
                        LibraryTab.search(it)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Library -> LibraryTab
                            // AM (RECENTS) -->
                            is Tab.Recents -> {
                                if (it.toHistory) {
                                    RecentsTab.showHistory()
                                } else {
                                    RecentsTab.showUpdates()
                                }
                                RecentsTab
                            }
                            // <-- AM (RECENTS)
                            // AM (BROWSE) -->
                            is Tab.Browse -> {
                                if (it.toExtensions) {
                                    BrowseTab.showExtension()
                                }
                                BrowseTab
                            }
                            // <-- AM (BROWSE)
                            is Tab.More -> MoreTab
                        }

                        if (it is Tab.Library && it.animeIdToOpen != null) {
                            navigator.push(AnimeScreen(it.animeIdToOpen))
                        }
                        if (it is Tab.More && it.toDownloads) {
                            navigator.push(DownloadQueueScreen)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NavigationSuiteItem(
        tab: eu.kanade.presentation.util.Tab,
        navigationSuiteType: NavigationSuiteType,
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        val modernPalette = LocalModernVisualPalette.current
        val chrome = resolveModernLibraryChrome(modernPalette, LocalModernArtworkIdentity.current)
        val useMiyorareVisuals = chrome != null
        val usePhoneGeometry = useMiyorareVisuals && navigationSuiteType == NavigationSuiteType.NavigationBar
        val iconSize = when {
            usePhoneGeometry && selected -> ModernPhoneNavigationGeometry.SelectedIconSize
            usePhoneGeometry -> ModernPhoneNavigationGeometry.IconSize
            selected -> 25.dp
            else -> 23.dp
        }
        NavigationSuiteItem(
            modifier = if (useMiyorareVisuals && selected) {
                Modifier.drawWithCache {
                    val horizontalInset = 2.dp.toPx()
                    val targetHeight = if (navigationSuiteType == NavigationSuiteType.NavigationBar) {
                        ModernPhoneNavigationGeometry.SelectedHeight.toPx().coerceAtMost(size.height)
                    } else {
                        size.height
                    }
                    val top = (size.height - targetHeight) / 2f
                    val surfaceWidth = (size.width - horizontalInset * 2f).coerceAtLeast(0f)
                    val selectedBrush = Brush.horizontalGradient(listOf(chrome!!.selectedStart, chrome.selectedEnd))
                    val selectedPath = when (
                        val outline = chrome.navSelectedShape.createOutline(
                            androidx.compose.ui.geometry.Size(surfaceWidth, targetHeight),
                            layoutDirection,
                            this,
                        )
                    ) {
                        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
                        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                        is Outline.Generic -> outline.path
                    }
                    val borderWidth = if (usePhoneGeometry) {
                        ModernPhoneNavigationGeometry.BorderWidth.toPx()
                    } else {
                        1.dp.toPx()
                    }
                    onDrawBehind {
                        translate(left = horizontalInset, top = top) {
                            drawPath(path = selectedPath, brush = selectedBrush)
                            drawPath(
                                path = selectedPath,
                                color = chrome.selectedBorder,
                                style = Stroke(width = borderWidth),
                            )
                        }
                    }
                }
            } else {
                Modifier
            },
            navigationSuiteType = navigationSuiteType,
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = {
                if (useMiyorareVisuals) {
                    if (tab is RecentsTab) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = tab.options.title,
                            tint = if (selected) chrome!!.selectedContent else chrome!!.content,
                            modifier = Modifier.size(iconSize),
                        )
                    } else {
                        Icon(
                            painter = tab.options.icon!!,
                            contentDescription = tab.options.title,
                            tint = if (selected) chrome!!.selectedContent else chrome!!.content,
                            modifier = Modifier.size(iconSize),
                        )
                    }
                } else {
                    Icon(
                        painter = tab.options.icon!!,
                        contentDescription = tab.options.title,
                    )
                }
            },
            label = {
                if (useMiyorareVisuals) {
                    Text(
                        text = tab.options.title,
                        modifier = Modifier.padding(bottom = if (selected) 6.dp else 0.dp),
                        color = if (selected) chrome!!.selectedContent else chrome!!.content,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = if (usePhoneGeometry) ModernPhoneNavigationGeometry.LabelSize else 14.sp,
                            lineHeight = if (usePhoneGeometry) ModernPhoneNavigationGeometry.LabelLineHeight else 17.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = tab.options.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            badge = tabBadge(tab),
        )
    }

    @Composable
    private fun tabBadge(tab: eu.kanade.presentation.util.Tab): (@Composable () -> Unit)? {
        val context = LocalContext.current
        val count by produceState(initialValue = 0, tab) {
            val graph = context.appGraph
            when (tab) {
                is RecentsTab -> {
                    combine(
                        graph.libraryPreferences.newShowUpdatesCount.changes(),
                        graph.libraryPreferences.newUpdatesCount.changes(),
                    ) { show, count ->
                        if (show) count else 0
                    }
                        .collectLatest { value = it }
                }

                is BrowseTab -> {
                    graph.sourcePreferences.extensionUpdatesCount.changes()
                        .collectLatest { value = it }
                }

                else -> value = 0
            }
        }
        if (count <= 0) return null
        return {
            val badgeContent: @Composable RowScope.() -> Unit = {
                val desc = when (tab) {
                    is RecentsTab -> pluralStringResource(
                        MR.plurals.notification_chapters_generic,
                        count = count,
                        count,
                    )

                    is BrowseTab -> pluralStringResource(
                        MR.plurals.update_check_notification_ext_updates,
                        count = count,
                        count,
                    )

                    else -> null
                }
                Text(
                    text = count.toString(),
                    modifier = Modifier.semantics {
                        if (desc != null) contentDescription = desc
                    },
                )
            }
            if (
                tab is RecentsTab &&
                resolveModernLibraryChrome(
                    LocalModernVisualPalette.current,
                    LocalModernArtworkIdentity.current,
                ) != null
            ) {
                Badge(
                    containerColor = Color(0xFFFFDDEB),
                    contentColor = Color(0xFF5E254B),
                    content = badgeContent,
                )
            } else {
                Badge(content = badgeContent)
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
        data class Library(val animeIdToOpen: Long? = null) : Tab

        // AM (RECENTS) -->
        data class Recents(val toHistory: Boolean) : Tab

        // <-- AM (RECENTS)
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
