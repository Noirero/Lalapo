package eu.kanade.tachiyomi.ui.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

        var manageSets by remember { mutableStateOf(false) }
        var pendingDelete by remember { mutableStateOf<Long?>(null) }
        val sets by viewModel.categorySets.collectAsStateWithLifecycle()
        val activeSetKey by viewModel.activeSetKey.collectAsStateWithLifecycle()

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
            var name by remember { mutableStateOf("") }
            var renameId by remember { mutableStateOf<Long?>(null) }
            AlertDialog(
                onDismissRequest = { manageSets = false },
                title = { Text("Kelola Set") },
                text = {
                    Column {
                        sets.forEachIndexed { index, set ->
                            Row {
                                TextButton(onClick = { viewModel.selectSet(set.syncKey) }) {
                                    Text(if (activeSetKey == set.syncKey) "✓ ${set.name}" else set.name)
                                }
                                TextButton(onClick = { renameId = set.id; name = set.name }) {
                                    Text("Ubah")
                                }
                                if (sets.size > 1) {
                                    TextButton(onClick = { pendingDelete = set.id }) { Text("Hapus") }
                                }
                                TextButton(onClick = {
                                    if (index > 0) {
                                        val ids = sets.map { it.id }.toMutableList()
                                        ids.add(index - 1, ids.removeAt(index))
                                        viewModel.reorderSets(ids)
                                    }
                                }) { Text("↑") }
                            }
                        }
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Set") })
                        Button(onClick = {
                            val id = renameId
                            if (id == null) viewModel.createSet(name) else viewModel.renameSet(id, name)
                            name = ""
                            renameId = null
                        }) { Text(if (renameId == null) "Buat Set" else "Simpan nama") }
                    }
                },
                confirmButton = { TextButton(onClick = { manageSets = false }) { Text("Selesai") } },
            )
        }

        pendingDelete?.let { sourceId ->
            val destination = sets.firstOrNull { it.id != sourceId }
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Hapus Set?") },
                text = { Text("Kategori dalam Set ini akan dipindahkan ke Set tujuan, bukan dihapus.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSet(sourceId, destination?.id)
                        pendingDelete = null
                    }) { Text("Pindahkan dan hapus") }
                },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Batal") } },
            )
        }

        when (val dialog = successState.dialog) {
            null -> {}
            CategoryDialog.Create -> {
                CategoryCreateDialog(
                    onDismissRequest = viewModel::dismissDialog,
                    onCreate = viewModel::createCategory,
                    categories = successState.categories.fastMap { it.name },
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
