package eu.kanade.tachiyomi.ui.history.anime

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.entries.anime.DuplicateAnimeDialog
import eu.kanade.presentation.history.HistoryDeleteAllDialog
import eu.kanade.presentation.history.HistoryDeleteDialog
import eu.kanade.presentation.history.anime.AnimeHistoryScreen
import eu.kanade.tachiyomi.ui.browse.anime.migration.search.MigrateAnimeDialog
import eu.kanade.tachiyomi.ui.browse.anime.migration.search.MigrateAnimeDialogScreenModel
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import uy.kohesive.injekt.injectLazy

val resumeLastEpisodeSeenEvent = Channel<Unit>()

@Composable
fun AnimeHistoryHalfTab(
    screenModel: AnimeHistoryScreenModel,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val state by screenModel.state.collectAsState()

    AnimeHistoryScreen(
        state = state,
        onClickCover = { navigator.push(AnimeScreen(it)) },
        onClickResume = screenModel::getNextEpisodeForAnime,
        onDialogChange = screenModel::setDialog,
        onClickFavorite = screenModel::addFavorite,
        contentPadding = contentPadding,
    )

    val onDismissRequest = { screenModel.setDialog(null) }
    when (val dialog = state.dialog) {
        is AnimeHistoryScreenModel.Dialog.Delete -> {
            HistoryDeleteDialog(
                onDismissRequest = onDismissRequest,
                onDelete = { all ->
                    if (all) {
                        screenModel.removeAllFromHistory(dialog.history.animeId)
                    } else {
                        screenModel.removeFromHistory(dialog.history)
                    }
                },
                isManga = false,
            )
        }
        is AnimeHistoryScreenModel.Dialog.DeleteAll -> {
            HistoryDeleteAllDialog(
                onDismissRequest = onDismissRequest,
                onDelete = screenModel::removeAllHistory,
            )
        }
        is AnimeHistoryScreenModel.Dialog.DuplicateAnime -> {
            DuplicateAnimeDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    screenModel.addFavorite(dialog.anime)
                },
                onOpenAnime = { navigator.push(AnimeScreen(dialog.duplicate.id)) },
                onMigrate = {
                    screenModel.showMigrateDialog(dialog.anime, dialog.duplicate)
                },
            )
        }
        is AnimeHistoryScreenModel.Dialog.ChangeCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = onDismissRequest,
                onEditCategories = { navigator.push(CategoryScreen()) },
                onConfirm = { include, _ ->
                    screenModel.moveAnimeToCategoriesAndAddToLibrary(dialog.anime, include)
                },
            )
        }
        is AnimeHistoryScreenModel.Dialog.Migrate -> {
            MigrateAnimeDialog(
                oldAnime = dialog.oldAnime,
                newAnime = dialog.newAnime,
                screenModel = MigrateAnimeDialogScreenModel(),
                onDismissRequest = onDismissRequest,
                onClickTitle = { navigator.push(AnimeScreen(dialog.oldAnime.id)) },
                onPopScreen = { navigator.replace(AnimeScreen(dialog.oldAnime.id)) },
            )
        }
        null -> {}
    }

    LaunchedEffect(state.list) {
        if (state.list != null) {
            (context as? MainActivity)?.ready = true
        }
    }

    LaunchedEffect(Unit) {
        screenModel.events.collectLatest { e ->
            when (e) {
                AnimeHistoryScreenModel.Event.InternalError ->
                    snackbarHostState.showSnackbar(context.stringResource(MR.strings.internal_error))
                AnimeHistoryScreenModel.Event.HistoryCleared ->
                    snackbarHostState.showSnackbar(context.stringResource(MR.strings.clear_history_completed))
                is AnimeHistoryScreenModel.Event.OpenEpisode -> openEpisode(context, e.episode, snackbarHostState)
            }
        }
    }
}

suspend fun openEpisode(context: Context, episode: Episode?, snackbarHostState: SnackbarHostState) {
    val playerPreferences: PlayerPreferences by injectLazy()
    val extPlayer = playerPreferences.alwaysUseExternalPlayer().get()
    if (episode != null) {
        MainActivity.startPlayerActivity(context, episode.animeId, episode.id, extPlayer)
    } else {
        snackbarHostState.showSnackbar(context.stringResource(AYMR.strings.no_next_episode))
    }
}
