package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.LocalModernArtworkIdentity
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernLibraryGeometry
import eu.kanade.presentation.theme.resolveModernLibraryChrome
import tachiyomi.presentation.core.components.FastScrollLazyVerticalGrid
import tachiyomi.presentation.core.util.plus

@Composable
internal fun LazyLibraryGrid(
    modifier: Modifier = Modifier,
    columns: Int,
    contentPadding: PaddingValues,
    content: LazyGridScope.() -> Unit,
) {
    val useModernGeometry = resolveModernLibraryChrome(
        LocalModernVisualPalette.current,
        LocalModernArtworkIdentity.current,
    ) != null
    FastScrollLazyVerticalGrid(
        columns = libraryGridCells(columns),
        modifier = modifier,
        contentPadding = contentPadding + if (useModernGeometry) {
            PaddingValues(horizontal = ModernLibraryGeometry.GridPadding, vertical = 8.dp)
        } else {
            PaddingValues(8.dp)
        },
        verticalArrangement = Arrangement.spacedBy(
            if (useModernGeometry) ModernLibraryGeometry.GridSpacing else CommonAnimeItemDefaults.GridVerticalSpacer,
        ),
        horizontalArrangement = Arrangement.spacedBy(
            if (useModernGeometry) ModernLibraryGeometry.GridSpacing else CommonAnimeItemDefaults.GridHorizontalSpacer,
        ),
        content = content,
    )
}

// Keep the existing preference contract: zero is adaptive; every positive value is fixed.
internal fun libraryGridCells(columns: Int): GridCells =
    if (columns == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(columns)

internal fun LazyGridScope.globalSearchItem(
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    if (!searchQuery.isNullOrEmpty()) {
        item(
            span = { GridItemSpan(maxLineSpan) },
            contentType = { "library_global_search_item" },
        ) {
            GlobalSearchItem(
                searchQuery = searchQuery,
                onClick = onGlobalSearchClicked,
            )
        }
    }
}
