package eu.kanade.tachiyomi.ui.updates

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.components.NavigatorAdaptiveSheet
import eu.kanade.presentation.entries.anime.EpisodeOptionsDialogScreen
import eu.kanade.presentation.updates.UpdatesDeleteConfirmationDialog
import eu.kanade.presentation.updates.anime.AnimeUpdateScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connections.discord.DiscordScreen
import eu.kanade.tachiyomi.ui.download.AnimeDownloadQueueScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.updates.anime.AnimeUpdatesItem
import eu.kanade.tachiyomi.ui.updates.anime.AnimeUpdatesScreenModel
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.core.i18n.stringResource
import tachiyomi.core.util.lang.launchIO
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data class UpdatesTab(
    private val externalPlayer: Boolean,
) : Tab() {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 1u,
                title = stringResource(MR.strings.label_recent_updates),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(AnimeDownloadQueueScreen)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AnimeUpdatesScreenModel() }
        val state by screenModel.state.collectAsState()
        val scope = rememberCoroutineScope()

        suspend fun openEpisode(updateItem: AnimeUpdatesItem, altPlayer: Boolean = false) {
            val update = updateItem.update
            val extPlayer = externalPlayer != altPlayer
            MainActivity.startPlayerActivity(context, update.animeId, update.episodeId, extPlayer)
        }

        AnimeUpdateScreen(
            state = state,
            snackbarHostState = screenModel.snackbarHostState,
            lastUpdated = screenModel.lastUpdated,
            relativeTime = screenModel.relativeTime,
            onClickCover = { item -> navigator.push(AnimeScreen(item.update.animeId)) },
            onSelectAll = screenModel::toggleAllSelection,
            onInvertSelection = screenModel::invertSelection,
            onUpdateLibrary = screenModel::updateLibrary,
            onDownloadEpisode = screenModel::downloadEpisodes,
            onMultiBookmarkClicked = screenModel::bookmarkUpdates,
            onMultiFillermarkClicked = screenModel::fillermarkUpdates,
            onMultiMarkAsSeenClicked = screenModel::markUpdatesSeen,
            onMultiDeleteClicked = screenModel::showConfirmDeleteEpisodes,
            onUpdateSelected = screenModel::toggleSelection,
            onOpenEpisode = { updateItem: AnimeUpdatesItem, altPlayer: Boolean ->
                scope.launchIO {
                    openEpisode(updateItem, altPlayer)
                }
                Unit
            },
        )

        val onDismissDialog = { screenModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is AnimeUpdatesScreenModel.Dialog.DeleteConfirmation -> {
                UpdatesDeleteConfirmationDialog(
                    onDismissRequest = onDismissDialog,
                    onConfirm = { screenModel.deleteEpisodes(dialog.toDelete) },
                    isManga = false,
                )
            }
            is AnimeUpdatesScreenModel.Dialog.ShowQualities -> {
                EpisodeOptionsDialogScreen.onDismissDialog = onDismissDialog
                NavigatorAdaptiveSheet(
                    screen = EpisodeOptionsDialogScreen(
                        useExternalDownloader = screenModel.useExternalDownloader,
                        episodeTitle = dialog.episodeTitle,
                        episodeId = dialog.episodeId,
                        animeId = dialog.animeId,
                        sourceId = dialog.sourceId,
                    ),
                    onDismissRequest = onDismissDialog,
                )
            }
            null -> {}
        }

        LaunchedEffect(Unit) {
            // AM (DISCORD) -->
            DiscordRPCService.setScreen(context, DiscordScreen.UPDATES)
            // <-- AM (DISCORD)
            screenModel.events.collectLatest { event ->
                when (event) {
                    AnimeUpdatesScreenModel.Event.InternalError -> screenModel.snackbarHostState.showSnackbar(
                        context.stringResource(MR.strings.internal_error),
                    )
                    is AnimeUpdatesScreenModel.Event.LibraryUpdateTriggered -> {
                        val stringRes = if (event.started) {
                            MR.strings.updating_library
                        } else {
                            MR.strings.update_already_running
                        }
                        screenModel.snackbarHostState.showSnackbar(context.stringResource(stringRes))
                    }
                }
            }
        }

        LaunchedEffect(state.selectionMode) {
            HomeScreen.showBottomNav(!state.selectionMode)
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }
        DisposableEffect(Unit) {
            screenModel.resetNewUpdatesCount()

            onDispose {
                screenModel.resetNewUpdatesCount()
            }
        }
    }
}
