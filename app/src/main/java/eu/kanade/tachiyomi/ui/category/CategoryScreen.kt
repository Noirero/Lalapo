package eu.kanade.tachiyomi.ui.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.category.CategoryScreen
import eu.kanade.presentation.category.components.CategoryCreateDialog
import eu.kanade.presentation.category.components.CategoryDeleteDialog
import eu.kanade.presentation.category.components.CategoryRenameDialog
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.presentation.core.screens.LoadingScreen

class CategoryScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<CategoryViewModel>()

        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state is CategoryScreenState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as CategoryScreenState.Success

        var manageSets by rememberSaveable { mutableStateOf(false) }
        val sets by viewModel.categorySets.collectAsStateWithLifecycle()
        val activeSetKey by viewModel.activeSetKey.collectAsStateWithLifecycle()
        val categoriesBySet by viewModel.categoriesBySet.collectAsStateWithLifecycle()
        val setBusy by viewModel.setBusy.collectAsStateWithLifecycle()
        val setError by viewModel.setError.collectAsStateWithLifecycle()

        Column {
            Button(onClick = { manageSets = true }, modifier = Modifier.padding(8.dp)) {
                Text("Kelola Set")
            }
            Box {
                CategoryScreen(
                    state = successState,
                    onClickCreate = { viewModel.showDialog(CategoryDialog.Create) },
                    onClickRename = { viewModel.showDialog(CategoryDialog.Rename(it)) },
                    onClickDelete = { viewModel.showDialog(CategoryDialog.Delete(it)) },
                    // AY -->
                    onClickHide = viewModel::hideCategory,
                    // <-- AY
                    onChangeOrder = viewModel::changeOrder,
                    navigateUp = navigator::pop,
                )
            }
        }

        if (manageSets) {
            CategorySetsSheet(
                sets = sets,
                activeSetKey = activeSetKey,
                categoriesBySet = categoriesBySet,
                busy = setBusy,
                error = setError,
                viewModel = viewModel,
                onClose = { manageSets = false },
            )
        }

        when (val dialog = successState.dialog) {
            null -> {}
            CategoryDialog.Create -> {
                CategoryCreateDialog(
                    onDismissRequest = viewModel::dismissDialog,
                    onCreate = viewModel::createCategory,
                    categories = successState.categories.fastMap { it.name },
                    busy = setBusy,
                    error = setError,
                )
            }
            is CategoryDialog.Rename -> {
                CategoryRenameDialog(
                    onDismissRequest = viewModel::dismissDialog,
                    onRename = { viewModel.renameCategory(dialog.category, it) },
                    categories = successState.categories.fastMap { it.name },
                    category = dialog.category.name,
                )
            }
            is CategoryDialog.Delete -> {
                CategoryDeleteDialog(
                    onDismissRequest = viewModel::dismissDialog,
                    onDelete = { viewModel.deleteCategory(dialog.category.id) },
                    category = dialog.category.name,
                )
            }
        }

        LaunchedEffect(Unit) {
            viewModel.events.collectLatest { event ->
                if (event is CategoryEvent.LocalizedMessage) {
                    context.toast(event.stringRes)
                }
            }
        }
    }
}
