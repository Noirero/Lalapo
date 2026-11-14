package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.anime.components.AnimeCover
import eu.kanade.presentation.components.modernCoverTreatment
import eu.kanade.presentation.theme.LocalModernArtworkIdentity
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernLibraryGeometry
import eu.kanade.presentation.theme.modernLibraryChromeCover
import eu.kanade.presentation.theme.modernLibraryChromeSurface
import eu.kanade.presentation.theme.resolveGlassPalette
import eu.kanade.presentation.theme.resolveModernLibraryChrome
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import tachiyomi.domain.anime.model.AnimeCover as AnimeCoverModel

object CommonAnimeItemDefaults {
    val GridHorizontalSpacer = 4.dp
    val GridVerticalSpacer = 4.dp

    @Suppress("ConstPropertyName")
    const val BrowseFavoriteCoverAlpha = 0.34f
}

@Composable
private fun libraryCoverAspectRatio(useModernLibraryTreatment: Boolean): Float {
    if (!useModernLibraryTreatment) return AnimeCover.Book.ratio
    return resolveModernLibraryChrome(
        LocalModernVisualPalette.current,
        LocalModernArtworkIdentity.current,
    )?.cardAspectRatio ?: AnimeCover.Book.ratio
}

@Composable
private fun libraryCoverShape(useModernLibraryTreatment: Boolean) = if (useModernLibraryTreatment) {
    resolveModernLibraryChrome(LocalModernVisualPalette.current, LocalModernArtworkIdentity.current)?.cardShape
        ?: RoundedCornerShape(14.dp)
} else {
    MaterialTheme.shapes.extraSmall
}

private val ContinueWatchingButtonSizeSmall = 28.dp
private val ContinueWatchingButtonSizeLarge = 32.dp

private val ContinueWatchingButtonIconSizeSmall = 16.dp
private val ContinueWatchingButtonIconSizeLarge = 20.dp

private val ContinueWatchingButtonGridPadding = 6.dp
private val ContinueWatchingButtonListSpacing = 8.dp

private const val GRID_SELECTED_COVER_ALPHA = 0.76f

/**
 * Layout of grid list item with title overlaying the cover.
 * Accepts null [title] for a cover-only view.
 */
@Composable
fun AnimeCompactGridItem(
    coverData: AnimeCoverModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean = false,
    title: String? = null,
    onClickContinueWatching: (() -> Unit)? = null,
    coverAlpha: Float = 1f,
    coverBadgeStart: @Composable (RowScope.() -> Unit)? = null,
    coverBadgeEnd: @Composable (RowScope.() -> Unit)? = null,
    useModernLibraryTreatment: Boolean = false,
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        useModernLibraryTreatment = useModernLibraryTreatment,
    ) {
        AnimeGridCover(
            cover = {
                AnimeCover.Book(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isSelected) GRID_SELECTED_COVER_ALPHA else coverAlpha),
                    data = coverData,
                    coverAspectRatio = libraryCoverAspectRatio(useModernLibraryTreatment),
                    shape = libraryCoverShape(useModernLibraryTreatment),
                )
            },
            badgesStart = coverBadgeStart,
            badgesEnd = coverBadgeEnd,
            useModernLibraryTreatment = useModernLibraryTreatment,
            content = {
                if (title != null) {
                    CoverTextOverlay(
                        title = title,
                        onClickContinueWatching = onClickContinueWatching,
                        useModernLibraryTreatment = useModernLibraryTreatment,
                    )
                } else if (onClickContinueWatching != null) {
                    ContinueWatchingButton(
                        size = ContinueWatchingButtonSizeLarge,
                        iconSize = ContinueWatchingButtonIconSizeLarge,
                        onClick = onClickContinueWatching,
                        modifier = Modifier
                            .padding(ContinueWatchingButtonGridPadding)
                            .align(Alignment.BottomEnd),
                    )
                }
            },
        )
    }
}

/**
 * Title overlay for [AnimeCompactGridItem]
 */
@Composable
private fun BoxScope.CoverTextOverlay(
    title: String,
    onClickContinueWatching: (() -> Unit)? = null,
    useModernLibraryTreatment: Boolean = false,
) {
    val palette = LocalModernVisualPalette.current
    val chrome = if (useModernLibraryTreatment) {
        resolveModernLibraryChrome(palette, LocalModernArtworkIdentity.current)
    } else {
        null
    }
    if (chrome != null) {
        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            CoverTextOverlayContent(
                title = title,
                onClickContinueWatching = onClickContinueWatching,
                useModernLibraryTreatment = true,
                titleSize = ModernLibraryGeometry.overlayTitleSize(maxWidth.value, maxHeight.value).sp,
            )
        }
    } else {
        CoverTextOverlayContent(title, onClickContinueWatching, useModernLibraryTreatment, 12.sp)
    }
}

@Composable
private fun BoxScope.CoverTextOverlayContent(
    title: String,
    onClickContinueWatching: (() -> Unit)?,
    useModernLibraryTreatment: Boolean,
    titleSize: androidx.compose.ui.unit.TextUnit,
) {
    val palette = LocalModernVisualPalette.current
    val glass = palette.resolveGlassPalette(MaterialTheme.colorScheme)
    val chrome = if (useModernLibraryTreatment) {
        resolveModernLibraryChrome(palette, LocalModernArtworkIdentity.current)
    } else {
        null
    }
    Box(
        modifier = Modifier
            .then(
                if (useModernLibraryTreatment) {
                    Modifier
                } else {
                    Modifier.clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                },
            )
            .background(
                if (useModernLibraryTreatment) {
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.46f to Color(0x3A000000),
                        1f to Color(0xD8000000),
                    )
                } else {
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color(0xAA000000),
                    )
                },
            )
            .then(
                if (chrome != null) {
                    Modifier.height(ModernLibraryGeometry.ScrimHeight)
                } else {
                    Modifier.fillMaxHeight(0.33f)
                },
            )
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
    )
    Row(
        modifier = Modifier.align(Alignment.BottomStart),
        verticalAlignment = Alignment.Bottom,
    ) {
        GridItemTitle(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            title = title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black,
                    blurRadius = 4f,
                ),
            ),
            minLines = 1,
            fontSize = titleSize,
        )
        if (onClickContinueWatching != null) {
            ContinueWatchingButton(
                size = ContinueWatchingButtonSizeSmall,
                iconSize = ContinueWatchingButtonIconSizeSmall,
                onClick = onClickContinueWatching,
                modifier = Modifier.padding(
                    end = ContinueWatchingButtonGridPadding,
                    bottom = ContinueWatchingButtonGridPadding,
                ),
            )
        }
    }
    if (useModernLibraryTreatment) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 4.dp)
                .width(28.dp)
                .height(3.dp)
                .background(chrome?.selectedBorder ?: glass.selectedBorder, RoundedCornerShape(2.dp)),
        )
    }
}

/**
 * Layout of grid list item with title below the cover.
 */
@Composable
fun AnimeComfortableGridItem(
    coverData: AnimeCoverModel,
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean = false,
    titleMaxLines: Int = 2,
    coverAlpha: Float = 1f,
    coverBadgeStart: (@Composable RowScope.() -> Unit)? = null,
    coverBadgeEnd: (@Composable RowScope.() -> Unit)? = null,
    onClickContinueWatching: (() -> Unit)? = null,
    useModernLibraryTreatment: Boolean = false,
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        useModernLibraryTreatment = useModernLibraryTreatment,
    ) {
        Column {
            AnimeGridCover(
                cover = {
                    AnimeCover.Book(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isSelected) GRID_SELECTED_COVER_ALPHA else coverAlpha),
                        data = coverData,
                        coverAspectRatio = libraryCoverAspectRatio(useModernLibraryTreatment),
                        shape = libraryCoverShape(useModernLibraryTreatment),
                    )
                },
                badgesStart = coverBadgeStart,
                badgesEnd = coverBadgeEnd,
                useModernLibraryTreatment = useModernLibraryTreatment,
                content = {
                    if (onClickContinueWatching != null) {
                        ContinueWatchingButton(
                            size = ContinueWatchingButtonSizeLarge,
                            iconSize = ContinueWatchingButtonIconSizeLarge,
                            onClick = onClickContinueWatching,
                            modifier = Modifier
                                .padding(ContinueWatchingButtonGridPadding)
                                .align(Alignment.BottomEnd),
                        )
                    }
                },
            )
            GridItemTitle(
                modifier = Modifier.padding(4.dp),
                title = title,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = titleMaxLines,
                fontSize = if (useModernLibraryTreatment) ModernLibraryGeometry.CoverTitleSize else 12.sp,
            )
        }
    }
}

/**
 * Common cover layout to add contents to be drawn on top of the cover.
 */
@Composable
private fun AnimeGridCover(
    modifier: Modifier = Modifier,
    cover: @Composable BoxScope.() -> Unit = {},
    badgesStart: (@Composable RowScope.() -> Unit)? = null,
    badgesEnd: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (BoxScope.() -> Unit)? = null,
    useModernLibraryTreatment: Boolean = false,
) {
    val chrome = if (useModernLibraryTreatment) {
        resolveModernLibraryChrome(LocalModernVisualPalette.current, LocalModernArtworkIdentity.current)
    } else {
        null
    }
    val coverShape = chrome?.cardShape ?: RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(chrome?.cardAspectRatio ?: AnimeCover.Book.ratio)
            .then(
                when {
                    chrome != null -> Modifier.modernLibraryChromeCover(chrome, coverShape)
                    useModernLibraryTreatment -> Modifier.modernCoverTreatment(coverShape)
                    else -> Modifier
                },
            ),
    ) {
        cover()
        content?.invoke(this)
        if (badgesStart != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopStart),
                content = badgesStart,
            )
        }

        if (badgesEnd != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd),
                content = badgesEnd,
            )
        }
    }
}

@Composable
private fun GridItemTitle(
    title: String,
    style: TextStyle,
    minLines: Int,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
) {
    Text(
        modifier = modifier,
        text = title,
        fontSize = fontSize,
        lineHeight = 18.sp,
        minLines = minLines,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = style,
    )
}

/**
 * Wrapper for grid items to handle selection state, click and long click.
 */
@Composable
private fun GridItemSelectable(
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    useModernLibraryTreatment: Boolean = false,
    content: @Composable () -> Unit,
) {
    val chrome = if (useModernLibraryTreatment) {
        resolveModernLibraryChrome(LocalModernVisualPalette.current, LocalModernArtworkIdentity.current)
    } else {
        null
    }
    Box(
        modifier = modifier
            .clip(chrome?.cardShape ?: MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .selectedOutline(isSelected = isSelected, color = MaterialTheme.colorScheme.secondary)
            .then(
                if (chrome != null) Modifier.semantics { selected = isSelected } else Modifier,
            )
            .padding(if (chrome != null) ModernLibraryGeometry.GridItemInset else 4.dp),
    ) {
        val contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            LocalContentColor.current
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * @see GridItemSelectable
 */
private fun Modifier.selectedOutline(
    isSelected: Boolean,
    color: Color,
) = drawBehind { if (isSelected) drawRect(color = color) }

/**
 * Layout of list item.
 */
@Composable
fun AnimeListItem(
    coverData: AnimeCoverModel,
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    badge: @Composable (RowScope.() -> Unit),
    isSelected: Boolean = false,
    coverAlpha: Float = 1f,
    onClickContinueWatching: (() -> Unit)? = null,
    // AY -->
    entries: Int = 0,
    containerHeight: Int = 0,
    modifier: Modifier = Modifier,
    // <-- AY
    useModernLibraryTreatment: Boolean = false,
) {
    val chrome = if (useModernLibraryTreatment) {
        resolveModernLibraryChrome(LocalModernVisualPalette.current, LocalModernArtworkIdentity.current)
    } else {
        null
    }
    Row(
        modifier = modifier
            .then(
                if (chrome != null) {
                    Modifier
                        .clip(chrome.cardShape)
                        .modernLibraryChromeSurface(chrome, chrome.cardShape, selected = isSelected)
                        .semantics { selected = isSelected }
                } else {
                    Modifier.selectedBackground(isSelected)
                },
            )
            .height(
                // AY -->
                when (entries) {
                    0 -> 76.dp
                    else -> {
                        val density = LocalDensity.current
                        with(density) { (containerHeight / entries).toDp() } - (3 / entries).dp
                    }
                },
                // <-- AY
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            // AY -->
            .padding(horizontal = if (chrome != null) ModernLibraryGeometry.ListInset else 16.dp, vertical = 3.dp),
        // <-- AY
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // AY -->
        AnimeCover.Book(
            // <-- AY
            modifier = Modifier
                .then(if (chrome != null) Modifier.heightIn(max = ModernLibraryGeometry.ListCoverSize) else Modifier)
                .fillMaxHeight()
                .alpha(coverAlpha),
            data = coverData,
            shape = libraryCoverShape(useModernLibraryTreatment),
            coverAspectRatio = if (chrome != null) AnimeCover.Square.ratio else AnimeCover.Book.ratio,
        )
        Text(
            text = title,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                chrome == null -> Color.Unspecified
                isSelected -> chrome.selectedContent
                else -> chrome.content
            },
        )
        BadgeGroup(content = badge)
        if (onClickContinueWatching != null) {
            ContinueWatchingButton(
                size = ContinueWatchingButtonSizeSmall,
                iconSize = ContinueWatchingButtonIconSizeSmall,
                onClick = onClickContinueWatching,
                modifier = Modifier.padding(start = ContinueWatchingButtonListSpacing),
            )
        }
    }
}

@Composable
private fun ContinueWatchingButton(
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        FilledIconButton(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
            ),
            modifier = Modifier.size(size),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(MR.strings.action_resume),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
