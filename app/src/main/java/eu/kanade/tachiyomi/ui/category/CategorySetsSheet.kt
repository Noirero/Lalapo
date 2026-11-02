package eu.kanade.tachiyomi.ui.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategorySet

/**
 * Only UI navigation and form inputs live here. Sets and category membership come
 * from repository-backed ViewModel flows; all mutations are serialized there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategorySetsSheet(
    sets: List<CategorySet>,
    activeSetKey: String,
    categoriesBySet: Map<Long, List<Category>>?,
    busy: Boolean,
    error: String?,
    viewModel: CategoryViewModel,
    onClose: () -> Unit,
) {
    var categorySetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editOpen by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editName by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var moveCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var moveTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var addCategoryOpen by rememberSaveable { mutableStateOf(false) }
    var categoryName by rememberSaveable { mutableStateOf("") }

    val managingSet = sets.firstOrNull { it.id == categorySetId }
    val managingCategories = managingSet?.let { categoriesBySet?.get(it.id).orEmpty() }.orEmpty()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val height = (LocalConfiguration.current.screenHeightDp * 0.86f).dp

    LaunchedEffect(Unit) { viewModel.clearSetError() }
    LaunchedEffect(sets, categorySetId) {
        if (categorySetId != null && managingSet == null) categorySetId = null
    }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onClose() },
        sheetState = sheetState,
        modifier = Modifier.testTag("category-sets-sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = height)
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (managingSet != null) {
                    IconButton(enabled = !busy, onClick = { categorySetId = null }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Kembali ke daftar Set")
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (managingSet == null) "Kelola Set" else managingSet.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (managingSet == null) {
                            "Atur Set, urutan, dan kategori tanpa menghapus koleksi."
                        } else {
                            "Kategori dalam Set ini, termasuk yang disembunyikan."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(enabled = !busy, onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Tutup Kelola Set")
                }
            }

            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            if (error != null) {
                Text(
                    error,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (categoriesBySet == null) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            } else if (managingSet == null) {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sets, key = { it.id }) { set ->
                        val index = sets.indexOfFirst { it.id == set.id }
                        var menuOpen by remember { mutableStateOf(false) }
                        Card(Modifier.fillMaxWidth().testTag("set-row:${set.id}")) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            set.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        val count = categoriesBySet[set.id].orEmpty().size
                                        Text(
                                            "$count kategori" + if (set.syncKey == activeSetKey) " · Aktif" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(enabled = !busy && index > 0, onClick = {
                                        val ids = sets.map { it.id }.toMutableList()
                                        ids.add(index - 1, ids.removeAt(index))
                                        viewModel.reorderSets(ids)
                                    }) {
                                        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Naikkan ${set.name}")
                                    }
                                    IconButton(enabled = !busy && index < sets.lastIndex, onClick = {
                                        val ids = sets.map { it.id }.toMutableList()
                                        ids.add(index + 1, ids.removeAt(index))
                                        viewModel.reorderSets(ids)
                                    }) {
                                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Turunkan ${set.name}")
                                    }
                                    androidx.compose.foundation.layout.Box {
                                        IconButton(enabled = !busy, onClick = { menuOpen = true }) {
                                            Icon(Icons.Outlined.MoreVert, contentDescription = "Tindakan untuk ${set.name}")
                                        }
                                        DropdownMenu(expanded = menuOpen && !busy, onDismissRequest = { menuOpen = false }) {
                                            if (set.syncKey != activeSetKey) {
                                                DropdownMenuItem(
                                                    text = { Text("Jadikan aktif") },
                                                    onClick = { menuOpen = false; viewModel.selectSet(set.syncKey) },
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Kelola kategori") },
                                                onClick = { menuOpen = false; categorySetId = set.id },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Ubah nama") },
                                                onClick = {
                                                    menuOpen = false
                                                    editId = set.id
                                                    editName = set.name
                                                    editOpen = true
                                                    viewModel.clearSetError()
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Hapus Set") },
                                                enabled = sets.size > 1,
                                                onClick = {
                                                    menuOpen = false
                                                    deleteId = set.id
                                                    deleteTargetId = null
                                                    viewModel.clearSetError()
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (sets.size == 1) {
                    Text(
                        "Set terakhir tidak dapat dihapus.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Button(
                    onClick = {
                        editId = null
                        editName = ""
                        editOpen = true
                        viewModel.clearSetError()
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("set-create"),
                ) { Text("Buat Set baru") }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (managingCategories.isEmpty()) {
                        item {
                            Text(
                                "Set ini belum memiliki kategori.",
                                modifier = Modifier.padding(vertical = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    items(managingCategories, key = { it.id }) { category ->
                        Card(Modifier.fillMaxWidth().testTag("set-category:${category.id}")) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(category.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        if (category.hidden) "Disembunyikan" else "Terlihat",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(
                                    enabled = !busy,
                                    onClick = { viewModel.toggleManagedCategoryVisibility(category) },
                                ) { Text(if (category.hidden) "Tampilkan" else "Sembunyikan") }
                                TextButton(
                                    enabled = !busy && sets.size > 1,
                                    onClick = {
                                        moveCategoryId = category.id
                                        moveTargetId = null
                                        viewModel.clearSetError()
                                    },
                                ) { Text("Pindah") }
                            }
                        }
                    }
                }
                Button(
                    enabled = !busy,
                    onClick = {
                        categoryName = ""
                        addCategoryOpen = true
                        viewModel.clearSetError()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("category-in-set-create"),
                ) { Text("Tambah kategori ke Set ini") }
            }
            TextButton(
                enabled = !busy,
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) { Text("Selesai") }
        }
    }

    if (editOpen) {
        val normalized = editName.trim()
        val duplicate = sets.any { it.id != editId && it.name.trim().equals(normalized, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { if (!busy) editOpen = false },
            title = { Text(if (editId == null) "Buat Set" else "Ubah nama Set") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it; viewModel.clearSetError() },
                        label = { Text("Nama Set") },
                        singleLine = true,
                        enabled = !busy,
                        isError = normalized.isEmpty() || duplicate,
                        supportingText = {
                            Text(
                                when {
                                    normalized.isEmpty() -> "Nama Set tidak boleh kosong."
                                    duplicate -> "Nama Set sudah digunakan."
                                    else -> "Nama harus unik."
                                },
                            )
                        },
                    )
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && normalized.isNotEmpty() && !duplicate, onClick = {
                    val id = editId
                    if (id == null) viewModel.createSet(normalized) { editOpen = false }
                    else viewModel.renameSet(id, normalized) { editOpen = false }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { editOpen = false }) { Text("Batal") }
            },
        )
    }

    if (addCategoryOpen && managingSet != null) {
        val normalized = categoryName.trim()
        val duplicate = managingCategories.any { it.name.equals(normalized, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { if (!busy) addCategoryOpen = false },
            title = { Text("Tambah kategori ke ${managingSet.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it; viewModel.clearSetError() },
                        label = { Text("Nama kategori") },
                        isError = normalized.isEmpty() || duplicate,
                        supportingText = {
                            if (normalized.isEmpty() || duplicate) {
                                Text(if (duplicate) "Kategori sudah ada dalam Set." else "Nama diperlukan.")
                            }
                        },
                        enabled = !busy,
                        singleLine = true,
                    )
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && normalized.isNotEmpty() && !duplicate, onClick = {
                    viewModel.createCategoryInSet(normalized, managingSet.id) { addCategoryOpen = false }
                }) { Text("Tambah") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { addCategoryOpen = false }) { Text("Batal") } },
        )
    }

    val deleted = sets.firstOrNull { it.id == deleteId }
    if (deleted != null) {
        val count = categoriesBySet?.let { it[deleted.id].orEmpty().size }
        val destinations = sets.filterNot { it.id == deleted.id }
        val validTarget = destinations.any { it.id == deleteTargetId }
        AlertDialog(
            onDismissRequest = { if (!busy) deleteId = null },
            title = { Text("Hapus Set ${deleted.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (count) {
                            null -> "Menghitung kategori…"
                            0 -> "Set kosong. Tidak ada kategori yang perlu dipindahkan."
                            else -> "$count kategori akan dipindahkan tanpa menghapus kategori atau anime."
                        },
                    )
                    if (count != null && count > 0) {
                        Text("Pilih Set tujuan:")
                        DestinationPicker(destinations, deleteTargetId, !busy) { deleteTargetId = it }
                    }
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && count != null && sets.size > 1 && (count == 0 || validTarget),
                    onClick = {
                        viewModel.deleteSet(deleted.id, if (count == 0) null else deleteTargetId) {
                            deleteId = null
                        }
                    },
                ) { Text(if (count == 0) "Hapus Set kosong" else "Pindahkan dan hapus") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleteId = null }) { Text("Batal") } },
        )
    }

    val moveCategory = managingCategories.firstOrNull { it.id == moveCategoryId }
    if (managingSet != null && moveCategory != null) {
        val destinations = sets.filterNot { it.id == managingSet.id }
        AlertDialog(
            onDismissRequest = { if (!busy) moveCategoryId = null },
            title = { Text("Pindahkan ${moveCategory.name}") },
            text = {
                Column {
                    Text("Pilih Set tujuan. Anime dalam kategori tetap tersimpan.")
                    DestinationPicker(destinations, moveTargetId, !busy) { moveTargetId = it }
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && destinations.any { it.id == moveTargetId },
                    onClick = {
                        val target = moveTargetId ?: return@TextButton
                        viewModel.moveCategory(moveCategory.id, target) { moveCategoryId = null }
                    },
                ) { Text("Pindahkan") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { moveCategoryId = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun DestinationPicker(
    destinations: List<CategorySet>,
    selectedId: Long?,
    enabled: Boolean,
    onSelected: (Long) -> Unit,
) {
    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
        destinations.forEach { set ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onSelected(set.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedId == set.id,
                    enabled = enabled,
                    onClick = { onSelected(set.id) },
                )
                Text(
                    set.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
