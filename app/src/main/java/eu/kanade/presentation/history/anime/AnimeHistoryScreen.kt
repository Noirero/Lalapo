package eu.kanade.presentation.history.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.history.anime.components.AnimeHistoryItem
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.ui.history.anime.AnimeHistoryScreenModel
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import java.time.LocalDate

@Composable
fun AnimeHistoryScreen(
    state: AnimeHistoryScreenModel.State,
    onClickCover: (animeId: Long) -> Unit,
    onClickResume: (animeId: Long, episodeId: Long) -> Unit,
    onClickFavorite: (animeId: Long) -> Unit,
    onDialogChange: (AnimeHistoryScreenModel.Dialog?) -> Unit,
    contentPadding: PaddingValues,
) {
    state.list.let {
        if (it == null) {
            LoadingScreen(Modifier.padding(contentPadding))
        } else if (it.isEmpty()) {
            val msg = if (!state.searchQuery.isNullOrEmpty()) {
                MR.strings.no_results_found
            } else {
                AYMR.strings.information_no_recent_anime
            }
            EmptyScreen(
                stringRes = msg,
                modifier = Modifier.padding(contentPadding),
            )
        } else {
            AnimeHistoryScreenContent(
                history = it,
                contentPadding = contentPadding,
                onClickCover = { history -> onClickCover(history.animeId) },
                onClickResume = { history -> onClickResume(history.animeId, history.episodeId) },
                onClickDelete = { item -> onDialogChange(AnimeHistoryScreenModel.Dialog.Delete(item)) },
                onClickFavorite = { history -> onClickFavorite(history.animeId) },
            )
        }
    }
}

@Composable
fun HistoryTopBar(
    state: AnimeHistoryScreenModel.State,
    onSearchQueryChange: (String?) -> Unit,
    onDialogChange: (AnimeHistoryScreenModel.Dialog?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    SearchToolbar(
        titleContent = { AppBarTitle(stringResource(MR.strings.history)) },
        searchQuery = state.searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        actions = {
            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.pref_clear_history),
                        icon = Icons.Outlined.DeleteSweep,
                        onClick = {
                            onDialogChange(AnimeHistoryScreenModel.Dialog.DeleteAll)
                        },
                    ),
                ),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun AnimeHistoryScreenContent(
    history: List<AnimeHistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (AnimeHistoryWithRelations) -> Unit,
    onClickResume: (AnimeHistoryWithRelations) -> Unit,
    onClickDelete: (AnimeHistoryWithRelations) -> Unit,
    onClickFavorite: (AnimeHistoryWithRelations) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = history,
            key = { "history-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is AnimeHistoryUiModel.Header -> "header"
                    is AnimeHistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is AnimeHistoryUiModel.Header -> {
                    ListGroupHeader(
                        modifier = Modifier.animateItemFastScroll(),
                        text = relativeDateText(item.date),
                    )
                }
                is AnimeHistoryUiModel.Item -> {
                    val value = item.item
                    AnimeHistoryItem(
                        modifier = Modifier.animateItemFastScroll(),
                        history = value,
                        onClickCover = { onClickCover(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { onClickDelete(value) },
                        onClickFavorite = { onClickFavorite(value) },
                    )
                }
            }
        }
    }
}

sealed interface AnimeHistoryUiModel {
    data class Header(val date: LocalDate) : AnimeHistoryUiModel
    data class Item(val item: AnimeHistoryWithRelations) : AnimeHistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(AnimeHistoryScreenModelStateProvider::class)
    historyState: AnimeHistoryScreenModel.State,
) {
    TachiyomiPreviewTheme {
        AnimeHistoryScreen(
            state = historyState,
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            onClickFavorite = {},
            contentPadding = PaddingValues(),
        )
    }
}
