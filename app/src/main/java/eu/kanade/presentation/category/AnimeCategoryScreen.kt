package eu.kanade.presentation.category

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.category.components.CategoryContent
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.ui.category.anime.AnimeCategoryScreenState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun AnimeCategoryScreen(
    state: AnimeCategoryScreenState.Success,
    onClickCreate: () -> Unit,
    onClickRename: (Category) -> Unit,
    onClickHide: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onClickMoveUp: (Category) -> Unit,
    onClickMoveDown: (Category) -> Unit,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.action_edit_categories),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            CategoryFloatingActionButton(
                lazyListState = lazyListState,
                onCreate = onClickCreate,
            )
        },
    ) { paddingValues ->
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.information_empty_category,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        CategoryContent(
            categories = state.categories,
            lazyListState = lazyListState,
            paddingValues = paddingValues + topSmallPaddingValues + PaddingValues(
                horizontal = MaterialTheme.padding.medium,
            ),
            onClickRename = onClickRename,
            onClickHide = onClickHide,
            onClickDelete = onClickDelete,
            onMoveUp = onClickMoveUp,
            onMoveDown = onClickMoveDown,
        )
    }
}
