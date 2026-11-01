package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.material.TabText

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    selectedIndex: Int,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return

    val currentIndex = selectedIndex.coerceIn(0, categories.lastIndex)
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(currentIndex)
    }

    LazyRow(
        modifier = modifier,
        state = listState,
    ) {
        itemsIndexed(
            items = categories,
            key = { _, category -> category.id },
        ) { index, category ->
            val selected = currentIndex == index
            TextButton(
                onClick = { onTabItemClick(index) },
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                TabText(
                    text = category.visualName,
                    badgeCount = getItemCountForCategory(category),
                )
            }
        }
    }
}
