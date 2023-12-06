package eu.kanade.tachiyomi.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.AnimeCategoryScreen
import eu.kanade.presentation.category.components.CategoryCreateDialog
import eu.kanade.presentation.category.components.CategoryDeleteDialog
import eu.kanade.presentation.category.components.CategoryRenameDialog
import eu.kanade.presentation.category.components.CategorySortAlphabeticallyDialog
import eu.kanade.tachiyomi.ui.category.anime.AnimeCategoryDialog
import eu.kanade.tachiyomi.ui.category.anime.AnimeCategoryEvent
import eu.kanade.tachiyomi.ui.category.anime.AnimeCategoryScreenModel
import eu.kanade.tachiyomi.ui.category.anime.AnimeCategoryScreenState
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.presentation.core.screens.LoadingScreen

class CategoryScreen : Screen {

    override val key = uniqueScreenKey

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AnimeCategoryScreenModel() }

        val state by screenModel.state.collectAsState()

        if (state is AnimeCategoryScreenState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as AnimeCategoryScreenState.Success

        AnimeCategoryScreen(
            state = successState,
            onClickCreate = { screenModel.showDialog(AnimeCategoryDialog.Create) },
            onClickRename = { screenModel.showDialog(AnimeCategoryDialog.Rename(it)) },
            onClickDelete = { screenModel.showDialog(AnimeCategoryDialog.Delete(it)) },
            onClickMoveUp = screenModel::moveUp,
            onClickMoveDown = screenModel::moveDown,
            onClickHide = screenModel::hideCategory,
            navigateUp = navigator::pop,
        )

        when (val dialog = successState.dialog) {
            null -> {}
            AnimeCategoryDialog.Create -> {
                CategoryCreateDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onCreate = { screenModel.createCategory(it) },
                    categories = successState.categories,
                )
            }
            is AnimeCategoryDialog.Rename -> {
                CategoryRenameDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onRename = { screenModel.renameCategory(dialog.category, it) },
                    categories = successState.categories,
                    category = dialog.category,
                )
            }
            is AnimeCategoryDialog.Delete -> {
                CategoryDeleteDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onDelete = { screenModel.deleteCategory(dialog.category.id) },
                    category = dialog.category,
                )
            }
            is AnimeCategoryDialog.SortAlphabetically -> {
                CategorySortAlphabeticallyDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    onSort = { screenModel.sortAlphabetically() },
                )
            }
        }

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
                if (event is AnimeCategoryEvent.LocalizedMessage) {
                    context.toast(event.stringRes)
                }
            }
        }
    }
}
