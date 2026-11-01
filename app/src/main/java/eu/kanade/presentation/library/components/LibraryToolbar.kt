package eu.kanade.presentation.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.modernBorderHighlight
import eu.kanade.presentation.components.modernSurfaceTreatment
import eu.kanade.presentation.theme.LocalModernArtworkIdentity
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernArtworkIdentity
import eu.kanade.presentation.theme.resolveGlassPalette
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
    val glass = modernPalette.resolveGlassPalette(MaterialTheme.colorScheme)
    val useMiyorareVisuals = isModern &&
        LocalModernArtworkIdentity.current != ModernArtworkIdentity.CUSTOM_WAVES
    val controlShape = RoundedCornerShape(22.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onClickSettings,
                        modifier = if (useMiyorareVisuals) {
                            Modifier
                                .size(44.dp)
                                .modernSurfaceTreatment(shape = controlShape)
                        } else {
                            Modifier
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(AYMR.strings.settings),
                            tint = if (useMiyorareVisuals) glass.content else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                title = {
                    OutlinedTextField(
                        value = searchQuery.orEmpty(),
                        onValueChange = { query ->
                            onSearchQueryChange(query.takeIf { it.isNotEmpty() })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (useMiyorareVisuals) Modifier.height(44.dp) else Modifier)
                            .then(
                                if (useMiyorareVisuals) {
                                    Modifier.modernSurfaceTreatment(shape = controlShape)
                                } else {
                                    Modifier
                                },
                            ),
                        singleLine = true,
                        colors = if (useMiyorareVisuals) {
                            OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                cursorColor = glass.selectedBorder,
                                focusedTextColor = glass.content,
                                unfocusedTextColor = glass.content,
                                focusedLeadingIconColor = glass.content,
                                unfocusedLeadingIconColor = glass.contentMuted,
                                focusedTrailingIconColor = glass.content,
                                unfocusedTrailingIconColor = glass.contentMuted,
                                focusedPlaceholderColor = glass.contentMuted,
                                unfocusedPlaceholderColor = glass.contentMuted,
                            )
                        } else if (isModern) {
                            OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.70f),
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
                    Box(
                        modifier = if (useMiyorareVisuals) {
                            Modifier
                                .size(44.dp)
                                .modernSurfaceTreatment(shape = controlShape)
                        } else {
                            Modifier
                        },
                        contentAlignment = Alignment.Center,
                    ) {
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
                    }
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
                            style = if (useMiyorareVisuals) {
                                MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontStyle = FontStyle.Italic,
                                )
                            } else {
                                MaterialTheme.typography.headlineSmall
                            },
                            color = if (useMiyorareVisuals) glass.content else Color.Unspecified,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isModern) {
                            Text(
                                text = " //",
                                color = if (useMiyorareVisuals) {
                                    glass.selectedBorder
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        }
                    }
                    Text(
                        text = "Koleksi anime kamu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (useMiyorareVisuals) glass.contentMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (title.numberOfAnime != null) {
                    if (useMiyorareVisuals) {
                        Surface(
                            modifier = Modifier.modernSurfaceTreatment(shape = controlShape),
                            shape = controlShape,
                            color = Color.Transparent,
                        ) {
                            Text(
                                text = "${title.numberOfAnime}",
                                color = glass.content,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        Pill(
                            text = "${title.numberOfAnime}",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = pillAlpha),
                            fontSize = 14.sp,
                        )
                    }
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
                    contentColor = if (isModern) glass.content else MaterialTheme.colorScheme.onSurface,
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
                                    if (useMiyorareVisuals) glass.borderStrong else modernPalette.borderHighlight
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
                horizontalArrangement = Arrangement.spacedBy(if (useMiyorareVisuals) 12.dp else 8.dp),
            ) {
                if (useMiyorareVisuals) {
                    ModernLibraryActionButton(
                        onClick = onClickFilter,
                        icon = Icons.Outlined.FilterList,
                        label = stringResource(MR.strings.action_filter),
                        selected = hasFilters,
                        modifier = Modifier.weight(1f),
                    )
                    ModernLibraryActionButton(
                        onClick = onClickSort,
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        label = stringResource(MR.strings.action_sort),
                        selected = false,
                        modifier = Modifier.weight(1f),
                    )
                } else {
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
}

@Composable
private fun RowScope.ModernLibraryActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalModernVisualPalette.current
    val glass = palette.resolveGlassPalette(MaterialTheme.colorScheme)
    val shape = RoundedCornerShape(24.dp)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .modernSurfaceTreatment(shape = shape, selected = selected),
        shape = shape,
        border = BorderStroke(0.dp, Color.Transparent),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = if (selected) glass.content else glass.contentMuted,
        ),
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            fontWeight = FontWeight.SemiBold,
        )
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
