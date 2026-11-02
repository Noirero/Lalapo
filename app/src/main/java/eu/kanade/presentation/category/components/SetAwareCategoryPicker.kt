package eu.kanade.presentation.category.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.category.CategoryPickerViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.category.model.Category
import kotlin.math.abs

/**
 * Pending decisions are kept separate from SQLDelight and survive recreation.
 * Encode category ID + boolean as signed Longs (IDs are always positive).
 */
@Composable
fun ChangeCategoryDialog(
    anime: List<Anime>,
    onDismissRequest: () -> Unit,
    onSaved: () -> Unit,
) {
    val picker = metroViewModel<CategoryPickerViewModel>()
    val sets by picker.sets.collectAsStateWithLifecycle()
    val categoriesBySet by picker.categoriesBySet.collectAsStateWithLifecycle()
    val activeKey by picker.activeKey.collectAsStateWithLifecycle()
    val animeIds = remember(anime) { anime.map { it.id }.distinct() }
    val scope = rememberCoroutineScope()
    var baseline by remember(animeIds) { mutableStateOf<Map<Long, Int>?>(null) }
    var pending by rememberSaveable(animeIds) { mutableStateOf(longArrayOf()) }
    var pageMemory by rememberSaveable(animeIds) { mutableStateOf(longArrayOf()) }
    var query by rememberSaveable(animeIds) { mutableStateOf("") }
    var showPreview by rememberSaveable(animeIds) { mutableStateOf(false) }
    var createOpen by rememberSaveable(animeIds) { mutableStateOf(false) }
    var createTargetSetId by rememberSaveable(animeIds) { mutableStateOf<Long?>(null) }
    var newName by rememberSaveable(animeIds) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectorOpen by remember { mutableStateOf(false) }

    LaunchedEffect(animeIds) {
        baseline = null
        error = picker.invokeSafely { baseline = picker.membershipCounts(animeIds) }
    }
    val activeSet = sets.firstOrNull { it.syncKey == activeKey } ?: sets.firstOrNull()
    val allCategoryIds = categoriesBySet.orEmpty().values.flatten().mapTo(hashSetOf()) { it.id }
    LaunchedEffect(allCategoryIds, baseline) {
        if (baseline != null && sets.isNotEmpty() && categoriesBySet != null) {
            val valid = retainValidCategoryPending(pending, allCategoryIds)
            if (!valid.contentEquals(pending)) {
                pending = valid
                error = "Kategori yang telah dihapus dikeluarkan dari pilihan tertunda."
            }
        }
    }

    val changes = pending.associate { abs(it) to (it > 0) }
    val categories = activeSet?.let { categoriesBySet?.get(it.id) }.orEmpty()
    val filtered = categories.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val screenMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.90f).dp

    fun runMutation(action: suspend () -> Unit, success: () -> Unit = {}) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val failure = picker.invokeSafely(action)
                if (failure == null) success() else error = failure
            } catch (e: CancellationException) {
                throw e
            } finally {
                busy = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!busy) onDismissRequest() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp).heightIn(max = screenMaxHeight),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Kategori",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Show title over cover",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                        Switch(
                            checked = showPreview,
                            enabled = !busy,
                            onCheckedChange = { showPreview = it },
                            modifier = Modifier.semantics { contentDescription = "Show title over cover preview" },
                        )
                    }
                    if (showPreview) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (anime.size == 1) {
                                val item = anime.first()
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                    error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                                    contentDescription = "Cover ${item.title}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(width = 48.dp, height = 68.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                )
                            } else {
                                Box(
                                    Modifier.size(width = 48.dp, height = 68.dp)
                                        .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        RoundedCornerShape(10.dp),
                                    ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${anime.size}", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (anime.size == 1) anime.first().title else "${anime.size} anime dipilih",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text("Kategori", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Box {
                        TextButton(
                            enabled = !busy && sets.isNotEmpty(),
                            onClick = { selectorOpen = true },
                            modifier = Modifier.testTag("picker-set-selector"),
                        ) {
                            Text("Set: ${activeSet?.name ?: "Memuat…"}", maxLines = 2)
                        }
                        DropdownMenu(expanded = selectorOpen && !busy, onDismissRequest = { selectorOpen = false }) {
                            sets.forEach { set ->
                                DropdownMenuItem(
                                    text = { Text(set.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        selectorOpen = false
                                        if (activeSet?.id != set.id) {
                                            query = ""
                                            runMutation { picker.selectSet(set.syncKey) }
                                        }
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        enabled = !busy,
                        singleLine = true,
                        label = { Text("Cari kategori dalam Set ini") },
                        modifier = Modifier.fillMaxWidth().testTag("picker-search"),
                    )
                    if (error != null) {
                        Text(
                            error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(top = 8.dp),
                    ) {
                        val columns = if (filtered.size > 10) 2 else 1
                        val availableHeight = maxHeight.value.toInt().coerceAtLeast(50)
                        val capacity = pickerPageCapacity(availableHeight, filtered.size)
                        val pages = filtered.chunked(capacity).ifEmpty { listOf(emptyList()) }
                        val rememberedPage = pageMemory.toList().chunked(2)
                            .firstOrNull { it.size == 2 && it[0] == activeSet?.id }?.get(1)?.toInt() ?: 0
                        if (baseline == null || categoriesBySet == null) {
                            CircularProgressIndicator(Modifier.padding(24.dp))
                        } else {
                            key(activeSet?.id, query) {
                                val pager = rememberPagerState(
                                    initialPage = rememberedPage.coerceIn(0, pages.lastIndex),
                                    pageCount = { pages.size },
                                )
                                LaunchedEffect(pager, activeSet?.id, query) {
                                    snapshotFlow { pager.currentPage }.collect { page ->
                                        if (query.isEmpty() && activeSet != null) {
                                            val old = pageMemory.toList().chunked(2)
                                                .filter { it.size == 2 && it[0] != activeSet.id }
                                                .flatMap { it }
                                            pageMemory = (old + listOf(activeSet.id, page.toLong())).toLongArray()
                                        }
                                    }
                                }
                                LaunchedEffect(pages.size) {
                                    if (pager.currentPage > pages.lastIndex) pager.scrollToPage(pages.lastIndex)
                                }
                                Column {
                                    val displayedPage = pages[pager.currentPage.coerceIn(0, pages.lastIndex)]
                                    val visibleRows = if (columns == 1) {
                                        displayedPage.size
                                    } else {
                                        (displayedPage.size + 1) / 2
                                    }
                                    HorizontalPager(
                                        state = pager,
                                        modifier = Modifier.fillMaxWidth()
                                            .height((visibleRows * 44 + (visibleRows - 1).coerceAtLeast(0) * 6).dp)
                                            .testTag("picker-pages"),
                                        userScrollEnabled = !busy,
                                    ) { page ->
                                        val items = pages[page.coerceIn(0, pages.lastIndex)]
                                        val (left, right) = pickerColumnMajor(items)
                                        val columnsContent = if (columns == 1) listOf(items) else listOf(left, right)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            columnsContent.forEach { column ->
                                                Column(
                                                    Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    column.forEach { category ->
                                                        val state = pickerState(
                                                            category.id,
                                                            baseline.orEmpty(),
                                                            animeIds.size,
                                                            changes,
                                                        )
                                                        CategoryPickerRow(
                                                            category = category,
                                                            state = state,
                                                            enabled = !busy,
                                                            onToggle = {
                                                                pending = toggleCategoryPending(
                                                                    pending,
                                                                    category.id,
                                                                    state,
                                                                )
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (pages.size > 1) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(top = 8.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            pages.indices.forEach { index ->
                                                Box(
                                                    Modifier.padding(horizontal = 3.dp)
                                                        .size(if (index == pager.currentPage) 8.dp else 6.dp)
                                                        .background(
                                                            if (index == pager.currentPage) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.outlineVariant
                                                            },
                                                            CircleShape,
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(enabled = !busy, onClick = onDismissRequest) { Text("Batal") }
                        IconButton(
                            enabled = !busy && activeSet != null,
                            onClick = {
                                newName = ""
                                error = null
                                createTargetSetId = activeSet?.id
                                createOpen = true
                            },
                            modifier = Modifier.size(48.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                .testTag("picker-create-category"),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Tambah kategori")
                        }
                        Button(
                            onClick = {
                                runMutation(
                                    action = {
                                        picker.save(
                                            anime,
                                            changes,
                                            addToLibrary = anime.size == 1 && !anime.first().favorite,
                                        )
                                    },
                                    success = onSaved,
                                )
                            },
                            enabled = !busy && baseline != null && anime.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("picker-save"),
                        ) { Text(if (busy) "Menyimpan…" else "Oke") }
                    }
                }
            }
        }
    }

    if (createOpen) {
        val target = sets.firstOrNull { it.id == createTargetSetId }
        val name = newName.trim()
        val duplicate = target?.let { categoriesBySet?.get(it.id).orEmpty() }
            .orEmpty().any { it.name.trim().equals(name, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { if (!busy) createOpen = false },
            title = { Text("Tambah kategori · ${target?.name ?: "Set tidak tersedia"}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            error = null
                        },
                        enabled = !busy,
                        singleLine = true,
                        label = { Text("Nama kategori") },
                        isError = name.isEmpty() || duplicate || target == null,
                        supportingText = {
                            if (name.isEmpty() || duplicate) {
                                Text(
                                    if (duplicate) "Nama kategori sudah digunakan." else "Nama kategori wajib diisi.",
                                )
                            }
                        },
                    )
                    if (target == null) {
                        Text(
                            "Set asal sudah dihapus. Tutup dan pilih Set yang tersedia.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (error != null) {
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && target != null && name.isNotEmpty() && !duplicate,
                    onClick = {
                        val targetId = target?.id ?: return@TextButton
                        runMutation({ picker.createInSet(name, targetId) }) { createOpen = false }
                    },
                ) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { createOpen = false }) { Text("Batal") }
            },
        )
    }
}

internal fun retainValidCategoryPending(pending: LongArray, validIds: Set<Long>): LongArray =
    pending.filter { abs(it) in validIds }.toLongArray()

internal fun pickerPageCapacity(heightDp: Int, count: Int): Int {
    val columns = if (count > 10) 2 else 1
    fun capacity(height: Int): Int = (((height + 6) / 50).coerceIn(1, 10) * columns).coerceAtMost(20)
    val withoutDots = capacity(heightDp)
    return if (count > withoutDots) capacity(heightDp - 16) else withoutDots
}

internal fun <T> pickerColumnMajor(items: List<T>): Pair<List<T>, List<T>> {
    val split = (items.size + 1) / 2
    return items.take(split) to items.drop(split)
}

internal fun pickerState(
    categoryId: Long,
    counts: Map<Long, Int>,
    animeCount: Int,
    pending: Map<Long, Boolean>,
): ToggleableState = when (pending[categoryId]) {
    true -> ToggleableState.On
    false -> ToggleableState.Off
    null -> when (counts[categoryId] ?: 0) {
        0 -> ToggleableState.Off
        animeCount -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
}

internal fun toggleCategoryPending(
    current: LongArray,
    categoryId: Long,
    state: ToggleableState,
): LongArray = (current.filter { abs(it) != categoryId } +
    (if (state == ToggleableState.On) -categoryId else categoryId)).toLongArray()

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryPickerRow(
    category: Category,
    state: ToggleableState,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    var showFullName by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (state == ToggleableState.On) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().height(44.dp)
            .combinedClickable(enabled = enabled, onClick = onToggle, onLongClick = { showFullName = true })
            .testTag("picker-category:${category.id}"),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriStateCheckbox(
                state = state,
                enabled = enabled,
                onClick = onToggle,
                modifier = Modifier.size(32.dp)
                    .semantics { contentDescription = category.name },
            )
            Spacer(Modifier.width(4.dp))
            Text(
                category.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (category.hidden) {
                Icon(
                    Icons.Outlined.VisibilityOff,
                    contentDescription = "Kategori tersembunyi",
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
    if (showFullName) {
        AlertDialog(
            onDismissRequest = { showFullName = false },
            title = { Text("Nama kategori") },
            text = { Text(category.name) },
            confirmButton = {
                TextButton(onClick = { showFullName = false }) { Text("Tutup") }
            },
        )
    }
}
