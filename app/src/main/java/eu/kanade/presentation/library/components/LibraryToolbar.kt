package eu.kanade.presentation.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.modernBorderHighlight
import eu.kanade.presentation.components.modernSurfaceTreatment
import eu.kanade.presentation.theme.LocalModernArtworkIdentity
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernLibraryGeometry
import eu.kanade.presentation.theme.ModernLibraryTitleMotif
import eu.kanade.presentation.theme.modernLibraryActionSurface
import eu.kanade.presentation.theme.modernLibraryChromeSurface
import eu.kanade.presentation.theme.modernLibrarySetSelectorWidth
import eu.kanade.presentation.theme.resolveGlassPalette
import eu.kanade.presentation.theme.resolveModernLibraryChrome
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
    val chrome = resolveModernLibraryChrome(modernPalette, LocalModernArtworkIdentity.current)
    val useMiyorareVisuals = chrome != null
    val controlShape = chrome?.controlShape ?: RoundedCornerShape(22.dp)
    val setSelectorWeight = chrome?.setSelectorWeight ?: 0.42f
    val segmentContentColor = when {
        chrome != null -> chrome.content
        isModern -> glass.content
        else -> MaterialTheme.colorScheme.onSurface
    }
    val searchInteraction = remember { MutableInteractionSource() }
    val searchFocused by searchInteraction.collectIsFocusedAsState()
    val searchHeight = with(LocalDensity.current) {
        max(ModernLibraryGeometry.SearchHeight, MaterialTheme.typography.bodyLarge.lineHeight.toDp() + 8.dp)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onClickSettings,
                        modifier = if (useMiyorareVisuals) {
                            Modifier
                                .size(ModernLibraryGeometry.TouchTarget)
                                .padding(ModernLibraryGeometry.SearchTouchInset)
                                .modernLibraryChromeSurface(chrome!!, shape = controlShape)
                        } else {
                            Modifier
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(AYMR.strings.settings),
                            tint = chrome?.content ?: MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                title = {
                    if (useMiyorareVisuals) {
                        BasicTextField(
                            value = searchQuery.orEmpty(),
                            onValueChange = { query ->
                                onSearchQueryChange(query.takeIf { it.isNotEmpty() })
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = ModernLibraryGeometry.TouchTarget)
                                .padding(
                                    vertical =
                                    ModernLibraryGeometry.SearchTouchInset,
                                )
                                .height(searchHeight)
                                .modernLibraryChromeSurface(chrome!!, shape = controlShape, emphasized = searchFocused),
                            singleLine = true,
                            interactionSource = searchInteraction,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = chrome.content,
                            ),
                            cursorBrush = SolidColor(chrome.selectedBorder),
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 14.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = chrome!!.contentMuted,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        if (searchQuery.isNullOrEmpty()) {
                                            Text(
                                                text = stringResource(MR.strings.action_search_hint),
                                                color = chrome!!.contentMuted,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        innerTextField()
                                    }
                                    if (!searchQuery.isNullOrEmpty()) {
                                        IconButton(
                                            onClick = { onSearchQueryChange(null) },
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = stringResource(MR.strings.action_reset),
                                                tint = chrome!!.content,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    } else {
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
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(
                                        alpha = 0.86f,
                                    ),
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
                    }
                },
                actions = {
                    AppBarActions(
                        actions = listOf(
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
                        overflowIconTint = chrome?.content,
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = if (isModern) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = if (useMiyorareVisuals) {
                            Color.Transparent
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (useMiyorareVisuals) ModernLibraryGeometry.TitleMargin else 16.dp,
                        vertical = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title.text,
                            style = if (useMiyorareVisuals) {
                                MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = ModernLibraryGeometry.TitleSize,
                                    fontWeight = if (chrome!!.serifTitle) FontWeight.Bold else FontWeight.ExtraBold,
                                    fontFamily = if (chrome.serifTitle) FontFamily.Serif else FontFamily.SansSerif,
                                    fontStyle = FontStyle.Italic,
                                    shadow = Shadow(color = Color(0x99000000), blurRadius = 5f),
                                )
                            } else {
                                MaterialTheme.typography.headlineSmall
                            },
                            color = chrome?.titleColor ?: Color.Unspecified,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (chrome != null) {
                            ModernLibraryTitleMotif(
                                identity = LocalModernArtworkIdentity.current!!,
                                color = chrome.border,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        } else if (isModern) {
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
                        style = if (useMiyorareVisuals) {
                            MaterialTheme.typography.bodyMedium.copy(fontSize = ModernLibraryGeometry.SubtitleSize)
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (useMiyorareVisuals) {
                            chrome!!.headerSubtitleColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (title.numberOfAnime != null) {
                    if (useMiyorareVisuals) {
                        Surface(
                            modifier = Modifier.modernLibraryChromeSurface(chrome!!, shape = controlShape),
                            shape = controlShape,
                            color = Color.Transparent,
                        ) {
                            Text(
                                text = "${title.numberOfAnime}",
                                color = chrome!!.content,
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
                        .padding(horizontal = if (useMiyorareVisuals) ModernLibraryGeometry.CategoryMargin else 16.dp)
                        .then(
                            if (isModern) {
                                if (chrome != null) {
                                    Modifier.modernLibraryChromeSurface(chrome, shape = chrome.segmentedShape)
                                } else {
                                    Modifier.modernSurfaceTreatment(shape = RoundedCornerShape(28.dp))
                                }
                            } else {
                                Modifier
                            },
                        ),
                    shape = chrome?.segmentedShape ?: RoundedCornerShape(28.dp),
                    color = if (isModern) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = segmentContentColor,
                    border = if (isModern) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    BoxWithConstraints {
                        val selectedSetWidth = if (chrome != null && selectedSet != null && hasCategoryNavigation) {
                            modernLibrarySetSelectorWidth(maxWidth, setSelectorWeight)
                        } else {
                            null
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selectedSet != null) {
                                Box(
                                    modifier = when {
                                        !hasCategoryNavigation -> Modifier.fillMaxWidth()
                                        selectedSetWidth != null -> Modifier.width(selectedSetWidth)
                                        else -> Modifier.weight(setSelectorWeight)
                                    },
                                ) {
                                    TextButton(
                                        onClick = { setMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RectangleShape,
                                        colors = if (chrome != null) {
                                            ButtonDefaults.textButtonColors(contentColor = chrome.border)
                                        } else {
                                            ButtonDefaults.textButtonColors()
                                        },
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
                                        if (useMiyorareVisuals) chrome!!.border else modernPalette.borderHighlight
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
                                    modifier = when {
                                        selectedSet == null -> Modifier.fillMaxWidth()
                                        selectedSetWidth != null -> Modifier.weight(1f)
                                        else -> Modifier.weight(1f - setSelectorWeight)
                                    },
                                )
                            } else if (showCategoryPicker && selectedCategory != null) {
                                Box(
                                    modifier = when {
                                        selectedSet == null -> Modifier.fillMaxWidth()
                                        selectedSetWidth != null -> Modifier.weight(1f)
                                        else -> Modifier.weight(1f - setSelectorWeight)
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
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (useMiyorareVisuals) ModernLibraryGeometry.ScreenMargin else 16.dp,
                        vertical = 8.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(ModernLibraryGeometry.FilterGap),
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
    val chrome = requireNotNull(resolveModernLibraryChrome(palette, LocalModernArtworkIdentity.current))
    val shape = chrome.actionShape
    val filterHeight = with(LocalDensity.current) {
        max(ModernLibraryGeometry.FilterHeight, ModernLibraryGeometry.FilterLineHeight.toDp() + 12.dp)
    }
    // Reserve the touch bounds independently of the smaller visual button.
    Box(
        modifier = modifier.heightIn(min = ModernLibraryGeometry.TouchTarget),
        contentAlignment = Alignment.Center,
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(filterHeight)
                .semantics { this.selected = selected }
                .modernLibraryActionSurface(chrome, selected = selected),
            shape = shape,
            border = BorderStroke(0.dp, Color.Transparent),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = if (selected) chrome.selectedContent else chrome.content,
            ),
            contentPadding = PaddingValues(horizontal = 9.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(ModernLibraryGeometry.FilterIcon),
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 4.dp),
                fontSize = ModernLibraryGeometry.FilterText,
                lineHeight = ModernLibraryGeometry.FilterLineHeight,
                fontWeight = FontWeight.SemiBold,
            )
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
