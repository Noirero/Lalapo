// AM (RECENTS) -->
package eu.kanade.tachiyomi.ui.recents

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.history.anime.HistoryTopBar
import eu.kanade.presentation.updates.anime.UpdatesBottomBar
import eu.kanade.presentation.updates.anime.UpdatesTopBar
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connection.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connection.discord.DiscordScreen
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.anime.AnimeHistoryHalfTab
import eu.kanade.tachiyomi.ui.history.anime.AnimeHistoryScreenModel
import eu.kanade.tachiyomi.ui.history.anime.resumeLastEpisodeSeenEvent
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.updates.anime.AnimeUpdatesHalfTab
import eu.kanade.tachiyomi.ui.updates.anime.AnimeUpdatesScreenModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import mihon.feature.upcoming.anime.UpcomingAnimeScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.injectLazy

data object RecentsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_recents_enter)
            return TabOptions(
                index = 1u,
                title = stringResource(MR.strings.label_recent_recents),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        resumeLastEpisodeSeenEvent.send(Unit)
    }

    override suspend fun onReselectHold(navigator: Navigator) {
        navigator.push(DownloadQueueScreen)
    }

    private val switchToHistoryTabChannel = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

    fun showHistory() {
        switchToHistoryTabChannel.trySend(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val animeHistoryScreenModel = rememberScreenModel { AnimeHistoryScreenModel() }
        val animeUpdatesScreenModel = rememberScreenModel { AnimeUpdatesScreenModel() }
        // AM (TAB_HOLD) -->
        val snackbarHostState = SnackbarHostState()
        // <-- AM (TAB_HOLD)
        var showHistoryScreen by remember { mutableStateOf(false) }

        RecentsScaffold(
            showHistoryScreen = showHistoryScreen,
            shouldShowHistoryScreen = { showHistoryScreen = it },
            animeUpdatesScreenModel = animeUpdatesScreenModel,
            animeHistoryScreenModel = animeHistoryScreenModel,
            snackbarHostState = snackbarHostState,
        ) { contentPadding ->
            Crossfade(targetState = showHistoryScreen, label = "recents_crossfade") { showHistory ->
                if (!showHistory) {
                    AnimeUpdatesHalfTab(animeUpdatesScreenModel, contentPadding)
                } else {
                    AnimeHistoryHalfTab(animeHistoryScreenModel, snackbarHostState, contentPadding)
                }
            }
        }

        LaunchedEffect(Unit) {
            switchToHistoryTabChannel.receiveAsFlow().collectLatest { showHistoryScreen = true }
        }

        LaunchedEffect(Unit) {
            // AM (DISCORD_RPC) -->
            with(DiscordRPCService) {
                discordScope.launchIO { setScreen(context.applicationContext, DiscordScreen.RECENTS) }
            }
            // <-- AM (DISCORD_RPC)
            (context as? MainActivity)?.ready = true
            // AM (TAB_HOLD) -->
            resumeLastEpisodeSeenEvent.receiveAsFlow().collectLatest {
                openEpisode(context, animeHistoryScreenModel.getNextEpisode(), snackbarHostState)
            }
        }
    }
}

internal suspend fun openEpisode(context: Context, episode: Episode?, snackbarHostState: SnackbarHostState) {
    val playerPreferences: PlayerPreferences by injectLazy()
    val extPlayer = playerPreferences.alwaysUseExternalPlayer().get()
    if (episode != null) {
        MainActivity.startPlayerActivity(context, episode.animeId, episode.id, extPlayer)
    } else {
        snackbarHostState.showSnackbar(context.stringResource(AYMR.strings.no_next_episode))
    }
}
// <-- AM (TAB_HOLD)

@Composable
fun RecentsScaffold(
    showHistoryScreen: Boolean,
    shouldShowHistoryScreen: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    animeUpdatesScreenModel: AnimeUpdatesScreenModel,
    animeHistoryScreenModel: AnimeHistoryScreenModel,
    content: @Composable (PaddingValues) -> Unit,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()

    val updatesState by animeUpdatesScreenModel.state.collectAsState()
    val historyState by animeHistoryScreenModel.state.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { scrollBehavior ->
            Column {
                if (!showHistoryScreen) {
                    UpdatesTopBar(
                        onCalendarClicked = { navigator.push(UpcomingAnimeScreen()) },
                        onUpdateLibrary = animeUpdatesScreenModel::updateLibrary,
                        actionModeCounter = updatesState.selected.size,
                        onSelectAll = { animeUpdatesScreenModel.toggleAllSelection(true) },
                        onInvertSelection = animeUpdatesScreenModel::invertSelection,
                        onCancelActionMode = { animeUpdatesScreenModel.toggleAllSelection(false) },
                        scrollBehavior = scrollBehavior,
                    )
                } else {
                    HistoryTopBar(
                        state = historyState,
                        onSearchQueryChange = animeHistoryScreenModel::updateSearchQuery,
                        onDialogChange = animeHistoryScreenModel::setDialog,
                        scrollBehavior = scrollBehavior,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.padding.small),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    FilterChip(
                        selected = !showHistoryScreen,
                        onClick = { shouldShowHistoryScreen(false) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_updates_outline_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        },
                        label = { Text(text = stringResource(MR.strings.label_recent_updates)) },
                    )

                    FilterChip(
                        selected = showHistoryScreen,
                        onClick = { shouldShowHistoryScreen(true) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        },
                        label = { Text(text = stringResource(MR.strings.label_recent_manga)) },
                    )
                }
            }
        },
        bottomBar = {
            if (!showHistoryScreen) {
                UpdatesBottomBar(
                    selected = updatesState.selected,
                    onDownloadEpisode = animeUpdatesScreenModel::downloadEpisodes,
                    onMultiBookmarkClicked = animeUpdatesScreenModel::bookmarkUpdates,
                    onMultiFillermarkClicked = animeUpdatesScreenModel::fillermarkUpdates,
                    onMultiMarkAsSeenClicked = animeUpdatesScreenModel::markUpdatesSeen,
                    onMultiDeleteClicked = animeUpdatesScreenModel::showConfirmDeleteEpisodes,
                    onOpenEpisode = { updateItem, altPlayer ->
                        scope.launchIO {
                            eu.kanade.tachiyomi.ui.updates.anime.openEpisode(context, updateItem, altPlayer)
                        }
                    },
                )
            }
        },
    ) { contentPadding ->
        content(contentPadding)
    }
}

// <-- AM (RECENTS)
