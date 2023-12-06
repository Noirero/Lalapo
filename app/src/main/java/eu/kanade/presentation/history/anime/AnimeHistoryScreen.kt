package eu.kanade.presentation.history.anime

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.ui.history.anime.AnimeHistoryScreenModel
import tachiyomi.core.preference.InMemoryPreferenceStore
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.bottomSuperLargePaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

@Composable
fun AnimeHistoryScreen(
    state: AnimeHistoryScreenModel.State,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onClickCover: (animeId: Long) -> Unit,
    onClickResume: (animeId: Long, episodeId: Long) -> Unit,
    onDialogChange: (AnimeHistoryScreenModel.Dialog?) -> Unit,
    modifier: Modifier = Modifier,
    preferences: UiPreferences = Injekt.get(),
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SearchToolbar(
                titleContent = { AppBarTitle(stringResource(MR.strings.history)) },
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onSearchQueryChange,
                actions = {
                    IconButton(onClick = { onDialogChange(AnimeHistoryScreenModel.Dialog.DeleteAll) }) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(MR.strings.pref_clear_history),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!state.searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.information_no_recent_anime
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                AnimeHistoryContent(
                    history = it,
                    // AM (NAVPILL)>
                    contentPadding = contentPadding + bottomSuperLargePaddingValues,
                    onClickCover = { history -> onClickCover(history.animeId) },
                    onClickResume = { history -> onClickResume(history.animeId, history.episodeId) },
                    onClickDelete = { item ->
                        onDialogChange(
                            AnimeHistoryScreenModel.Dialog.Delete(item),
                        )
                    },
                    preferences = preferences,
                )
            }
        }
    }
}

sealed interface AnimeHistoryUiModel {
    data class Header(val date: Date) : AnimeHistoryUiModel
    data class Item(val item: AnimeHistoryWithRelations) : AnimeHistoryUiModel
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(AnimeHistoryScreenModelStateProvider::class)
    historyState: AnimeHistoryScreenModel.State,
) {
    TachiyomiTheme {
        AnimeHistoryScreen(
            state = historyState,
            snackbarHostState = SnackbarHostState(),
            onSearchQueryChange = {},
            onClickCover = {},
            onClickResume = { _, _ -> run {} },
            onDialogChange = {},
            preferences = UiPreferences(
                InMemoryPreferenceStore(
                    sequenceOf(
                        InMemoryPreferenceStore.InMemoryPreference(
                            key = "relative_time_v2",
                            data = false,
                            defaultValue = false,
                        ),
                    ),
                ),
            ),
        )
    }
}
