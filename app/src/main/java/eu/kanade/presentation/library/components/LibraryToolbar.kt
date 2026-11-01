package eu.kanade.presentation.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.modernBorderHighlight
import eu.kanade.presentation.components.modernSurfaceTreatment
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernHeaderArtwork
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategorySet
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.i18n.MR
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    selectedCount: Int,
    title: LibraryToolbarTitle,
    sets: List<CategorySet>,
    activeSetKey: String,
    categories: List<Category>,
    activeCategoryIndex: Int,
    showCategoryTabs: Boolean,
    groupType: Int,
    hasCategories: Boolean,
    getItemCountForCategory: (Category) -> Int?,
    onClickSettings: () -> Unit,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onSelectSet: (String) -> Unit,
    onManageSets: () -> Unit,
    onSelectCategory: (Int) -> Unit,
    onClickFilter: () -> Unit,
    onClickSort: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomAnime: () -> Unit,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
) = when {
    selectedCount > 0 -> LibrarySelectionToolbar(
        selectedCount = selectedCount,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
    )
    else -> LibraryRegularToolbar(
        title = title,
        hasFilters = hasActiveFilters,
        sets = sets,
        activeSetKey = activeSetKey,
        categories = categories,
        activeCategoryIndex = activeCategoryIndex,
        showCategoryTabs = showCategoryTabs,
        groupType = groupType,
        hasCategories = hasCategories,
        getItemCountForCategory = getItemCountForCategory,
        onClickSettings = onClickSettings,
        onSelectSet = onSelectSet,
        onManageSets = onManageSets,
        onSelectCategory = onSelectCategory,
        onClickFilter = onClickFilter,
        onClickSort = onClickSort,
        onClickRefresh = onClickRefresh,
        onClickGlobalUpdate = onClickGlobalUpdate,
        onClickOpenRandomAnime = onClickOpenRandomAnime,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun LibraryRegularToolbar(
    title: LibraryToolbarTitle,
    hasFilters: Boolean,
    sets: List<CategorySet>,
    activeSetKey: String,
    categories: List<Category>,
    activeCategoryIndex: Int,
    showCategoryTabs: Boolean,
    groupType: Int,
    hasCategories: Boolean,
    getItemCountForCategory: (Category) -> Int?,
    onClickSettings: () -> Unit,
    onSelectSet: (String) -> Unit,
    onManageSets: () -> Unit,
    onSelectCategory: (Int) -> Unit,
    onClickFilter: () -> Unit,
    onClickSort: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomAnime: () -> Unit,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
) {
    var setMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val selectedSet = sets.firstOrNull { it.syncKey == activeSetKey } ?: sets.firstOrNull()
    val selectedCategoryIndex = activeCategoryIndex.coerceIn(0, categories.lastIndex.coerceAtLeast(0))
    val selectedCategory = categories.getOrNull(selectedCategoryIndex)
    val categoryGroupLabel = when (groupType) {
        LibraryGroup.BY_SOURCE -> stringResource(MR.strings.label_sources)
        LibraryGroup.BY_STATUS -> stringResource(MR.strings.status)
        LibraryGroup.BY_TRACK_STATUS -> stringResource(AMMR.strings.tracking_status)
        LibraryGroup.UNGROUPED -> stringResource(AMMR.strings.group)
        else -> stringResource(MR.strings.categories)
    }
    val pillAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f
    val modernPalette = LocalModernVisualPalette.current
    val isModern = modernPalette.isModern

    Box(modifier = Modifier.fillMaxWidth()) {
        if (isModern) {
            ModernHeaderArtwork(modifier = Modifier.matchParentSize())
        }
        Column {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClickSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(AYMR.strings.settings),
                        )
                    }
                },
                title = {
                    OutlinedTextField(
                        value = searchQuery.orEmpty(),
                        onValueChange = { query ->
                            onSearchQueryChange(query.takeIf { it.isNotEmpty() })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = if (isModern) {
                            OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(
                                    alpha = 0.70f,
                                ),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f),
                                unfocusedBorderColor = modernPalette.borderHighlight,
                            )
                        } else {
                            OutlinedTextFieldDefaults.colors()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = if (!searchQuery.isNullOrEmpty()) {
                            {
                                IconButton(onClick = { onSearchQueryChange(null) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(MR.strings.action_reset),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        placeholder = {
                            Text(
                                text = stringResource(MR.strings.action_search_hint),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                },
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_update_library),
                                onClick = onClickGlobalUpdate,
                            ),
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_update_category),
                                onClick = onClickRefresh,
                            ),
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_open_random_manga),
                                onClick = onClickOpenRandomAnime,
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = if (isModern) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title.text,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isModern) {
                            Text(
                                text = " //",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        }
                    }
                    Text(
                        text = "Koleksi anime kamu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (title.numberOfAnime != null) {
                    Pill(
                        text = "${title.numberOfAnime}",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = pillAlpha),
                        fontSize = 14.sp,
                    )
                }
            }

            val showTabRail = showCategoryTabs && categories.size > 1
            val showCategoryPicker = !showCategoryTabs && selectedCategory != null
            val hasCategoryNavigation = showTabRail || showCategoryPicker

            if (selectedSet != null || hasCategoryNavigation) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .then(
                            if (isModern) {
                                Modifier.modernSurfaceTreatment(shape = RoundedCornerShape(28.dp))
                            } else {
                                Modifier
                            },
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = if (isModern) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = if (isModern) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selectedSet != null) {
                            Box(
                                modifier = if (hasCategoryNavigation) {
                                    Modifier.weight(0.42f)
                                } else {
                                    Modifier.fillMaxWidth()
                                },
                            ) {
                                TextButton(
                                    onClick = { setMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RectangleShape,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Label,
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = "Set: ${selectedSet.name}",
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                    )
                                }
                                DropdownMenu(
                                    expanded = setMenuExpanded,
                                    onDismissRequest = { setMenuExpanded = false },
                                ) {
                                    sets.forEach { set ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(if (set.id == selectedSet.id) "✓ ${set.name}" else set.name)
                                            },
                                            onClick = {
                                                setMenuExpanded = false
                                                onSelectSet(set.syncKey)
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Kelola Set") },
                                        onClick = {
                                            setMenuExpanded = false
                                            onManageSets()
                                        },
                                    )
                                }
                            }
                        }

                        if (selectedSet != null && hasCategoryNavigation) {
                            VerticalDivider(
                                modifier = Modifier.height(32.dp),
                                color = if (isModern) {
                                    modernPalette.borderHighlight
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            )
                        }

                        if (showTabRail) {
                            LibraryTabs(
                                categories = categories,
                                selectedIndex = selectedCategoryIndex,
                                getItemCountForCategory = getItemCountForCategory,
                                onTabItemClick = onSelectCategory,
                                modifier = if (selectedSet != null) {
                                    Modifier.weight(0.58f)
                                } else {
                                    Modifier.fillMaxWidth()
                                },
                            )
                        } else if (showCategoryPicker && selectedCategory != null) {
                            Box(
                                modifier = if (selectedSet != null) {
                                    Modifier.weight(0.58f)
                                } else {
                                    Modifier.fillMaxWidth()
                                },
                            ) {
                                TextButton(
                                    onClick = { categoryMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RectangleShape,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Layers,
                                        contentDescription = null,
                                    )
                                    Text(
                                        text = "$categoryGroupLabel: ${selectedCategory.visualName}",
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                    )
                                }
                                DropdownMenu(
                                    expanded = categoryMenuExpanded,
                                    onDismissRequest = { categoryMenuExpanded = false },
                                ) {
                                    categories.forEachIndexed { index, category ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (index == selectedCategoryIndex) {
                                                        "✓ ${category.visualName}"
                                                    } else {
                                                        category.visualName
                                                    },
                                                )
                                            },
                                            onClick = {
                                                categoryMenuExpanded = false
                                                onSelectCategory(index)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onClickFilter,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isModern) Modifier.modernBorderHighlight(RoundedCornerShape(24.dp)) else Modifier,
                        ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isModern) {
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f)
                        } else {
                            Color.Transparent
                        },
                        contentColor = if (hasFilters) {
                            MaterialTheme.colorScheme.active
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(MR.strings.action_filter),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(
                    onClick = onClickSort,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isModern) Modifier.modernBorderHighlight(RoundedCornerShape(24.dp)) else Modifier,
                        ),
                    colors = if (isModern) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f),
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(MR.strings.action_sort),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    AppBar(
        titleContent = { Text(text = "$selectedCount") },
        actions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = Icons.Outlined.SelectAll,
                        onClick = onClickSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = Icons.Outlined.FlipToBack,
                        onClick = onClickInvertSelection,
                    ),
                ),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
    )
}

@Immutable
data class LibraryToolbarTitle(
    val text: String,
    val numberOfAnime: Int? = null,
)
