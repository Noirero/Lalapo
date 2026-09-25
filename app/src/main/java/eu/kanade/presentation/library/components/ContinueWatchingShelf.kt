package eu.kanade.presentation.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.anime.components.AnimeCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryAnime
import tachiyomi.i18n.MR
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.domain.anime.model.AnimeCover as AnimeCoverModel

@Composable
fun ContinueWatchingShelf(
    items: List<LibraryItem>,
    onClickAnime: (LibraryAnime) -> Unit,
    onContinueWatching: (LibraryAnime) -> Unit,
) {
    if (items.isEmpty()) return

    Column(
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        Text(
            text = stringResource(AMMR.strings.am_label_continue_watching),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                items = items,
                key = { it.id },
                contentType = { "continue_watching_item" },
            ) { item ->
                val anime = item.libraryAnime.anime
                Column(
                    modifier = Modifier
                        .width(112.dp)
                        .clickable { onClickAnime(item.libraryAnime) }
                        .padding(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(AnimeCover.Book.ratio),
                    ) {
                        AnimeCover.Book(
                            modifier = Modifier.fillMaxWidth(),
                            data = AnimeCoverModel(
                                animeId = anime.id,
                                sourceId = anime.source,
                                isAnimeFavorite = anime.favorite,
                                url = anime.thumbnailUrl,
                                lastModified = anime.coverLastModified,
                            ),
                        )
                        FilledIconButton(
                            onClick = { onContinueWatching(item.libraryAnime) },
                            shape = MaterialTheme.shapes.small,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
                            ),
                            modifier = Modifier
                                .padding(6.dp)
                                .size(32.dp)
                                .align(Alignment.BottomEnd),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(MR.strings.action_resume),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        text = anime.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
