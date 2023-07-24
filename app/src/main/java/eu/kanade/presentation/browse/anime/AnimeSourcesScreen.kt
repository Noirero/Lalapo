package eu.kanade.presentation.browse.anime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapCalls
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.source.anime.model.AnimeSource
import eu.kanade.domain.source.anime.model.Pin
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.anime.components.BaseAnimeSourceItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.ExtendedFloatingActionButton
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.components.ScrollbarLazyColumn
import eu.kanade.presentation.theme.header
import eu.kanade.presentation.util.bottomSuperLargePaddingValues
import eu.kanade.presentation.util.collectAsState
import eu.kanade.presentation.util.isScrollingDown
import eu.kanade.presentation.util.padding
import eu.kanade.presentation.util.plus
import eu.kanade.presentation.util.topSmallPaddingValues
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.anime.LocalAnimeSource
import eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionsScreen
import eu.kanade.tachiyomi.ui.browse.anime.extension.details.AnimeExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.browse.anime.migration.sources.MigrateAnimeSourceScreen
import eu.kanade.tachiyomi.ui.browse.anime.source.AnimeSourcesFilterScreen
import eu.kanade.presentation.browse.anime.components.BaseAnimeSourceItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.anime.source.AnimeSourcesState
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreenModel.Listing
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.domain.source.anime.model.Pin
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.SecondaryItemAlpha
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.entries.anime.LocalAnimeSource

@Composable
fun AnimeSourcesScreen(
    state: AnimeSourcesState,
    // AM (BR) -->
    navigator: Navigator,
    // <-- AM (BR)
    onClickItem: (AnimeSource, Listing) -> Unit,
    onClickPin: (AnimeSource) -> Unit,
    onLongClickItem: (AnimeSource) -> Unit,
    // AM (BR) -->
    sourcePreferences: SourcePreferences,
    // <-- AM (BR)
) {
    // AM (BR) -->
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                titleContent = { AppBarTitle(stringResource(R.string.browse)) },
                actions = {
                    IconButton(onClick = { navigator.push(GlobalAnimeSearchScreen()) }) {
                        Icon(
                            Icons.Outlined.TravelExplore,
                            contentDescription = stringResource(R.string.action_global_search),
                        )
                    }
                    IconButton(onClick = { navigator.push(AnimeSourcesFilterScreen()) }) {
                        Icon(
                            Icons.Outlined.FilterList,
                            contentDescription = stringResource(R.string.action_filter),
                        )
                    }
                    IconButton(onClick = { navigator.push(MigrateAnimeSourceScreen()) }) {
                        Icon(
                            Icons.Outlined.SwapCalls,
                            contentDescription = stringResource(R.string.action_migrate),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        // <-- AM (BR)
        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.padding(contentPadding))
            state.isEmpty -> EmptyScreen(
                textResource = R.string.source_empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            else -> {
                val extensionsListState = rememberLazyListState()
                Scaffold(
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            // AM (BR) -->
                            val extensionUpdateCount by sourcePreferences.animeExtensionUpdatesCount().collectAsState()
                            val buttonText = if (extensionUpdateCount != 0) R.string.ext_update else R.string.ext_install
                            val buttonIcon = if (extensionUpdateCount != 0) Icons.Filled.Upload else Icons.Filled.Download
                            ExtendedFloatingActionButton(
                                text = { Text(text = stringResource(buttonText)) },
                                // <-- AM (BR)
                                icon = {
                                    Icon(
                                        imageVector = buttonIcon,
                                        contentDescription = null,
                                    )
                                },
                                onClick = { navigator.push(AnimeExtensionsScreen()) },
                                expanded = !(extensionsListState.isScrollingDown()) || extensionUpdateCount != 0,
                            )
                        }
                    },
                ) {
                    ScrollbarLazyColumn(
                        state = extensionsListState,
                        contentPadding = contentPadding + topSmallPaddingValues + bottomSuperLargePaddingValues,
                    ) {
                        items(
                            items = state.items,
                            contentType = {
                                when (it) {
                                    is AnimeSourceUiModel.Header -> "header"
                                    is AnimeSourceUiModel.Item -> "item"
                                }
                            },
                            key = {
                                when (it) {
                                    is AnimeSourceUiModel.Header -> it.hashCode()
                                    is AnimeSourceUiModel.Item -> "source-${it.source.key()}"
                                }
                            },
                        ) { model ->
                            when (model) {
                                is AnimeSourceUiModel.Header -> {
                                    AnimeSourceHeader(
                                        modifier = Modifier.animateItemPlacement(),
                                        language = model.language,
                                    )
                                }
                                is AnimeSourceUiModel.Item -> AnimeSourceItem(
                                    modifier = Modifier.animateItemPlacement(),
                                    navigator = navigator,
                                    source = model.source,
                                    onClickItem = onClickItem,
                                    onLongClickItem = onLongClickItem,
                                    onClickPin = onClickPin,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeSourceHeader(
    modifier: Modifier = Modifier,
    language: String,
) {
    val context = LocalContext.current
    Text(
        text = LocaleHelper.getSourceDisplayName(language, context),
        modifier = modifier
            .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        style = MaterialTheme.typography.header,
    )
}

@Composable
private fun AnimeSourceItem(
    modifier: Modifier = Modifier,
    // AM (BR) -->
    navigator: Navigator,
    // <-- AM (BR)
    source: AnimeSource,
    onClickItem: (AnimeSource, Listing) -> Unit,
    onLongClickItem: (AnimeSource) -> Unit,
    onClickPin: (AnimeSource) -> Unit,
) {
    BaseAnimeSourceItem(
        modifier = modifier,
        source = source,
        onClickItem = { onClickItem(source, Listing.Popular) },
        onLongClickItem = { onLongClickItem(source) },
        action = {
            if (source.supportsLatest) {
                // AM (BR) -->
                TextButton(
                    onClick = { onClickItem(source, Listing.Latest) },
                    modifier = if (source.id == LocalAnimeSource.ID) {
                        modifier.padding(end = 48.dp)
                    } else {
                        Modifier
                    },
                ) {
                    // <-- AM (BR)
                    Text(
                        text = stringResource(id = R.string.latest),
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            // AM (BR) -->
            if (source.id != LocalAnimeSource.ID) {
                AnimeSourceSettingsButton(
                    navigator = navigator,
                    source = source,
                )
            }
        },
        pin = {
            // <-- AM (BR)
            AnimeSourcePinButton(
                isPinned = Pin.Pinned in source.pin,
                onClick = { onClickPin(source) },
            )
        },
    )
}

@Composable
private fun AnimeSourcePinButton(
    isPinned: Boolean,
    onClick: () -> Unit,
) {
    val icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin
    val tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = SecondaryItemAlpha)
    val description = if (isPinned) R.string.action_unpin else R.string.action_pin
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            tint = tint,
            // AM (BR) -->
            modifier = Modifier
                .size(16.dp)
                .rotate(-30f),
            // <-- AM (BR)
            contentDescription = stringResource(description),
        )
    }
}

// AM (BR) -->
@Composable
private fun AnimeSourceSettingsButton(
    navigator: Navigator,
    source: AnimeSource,
) {
    IconButton(onClick = { navigator.push(AnimeExtensionDetailsScreen(source.installedExtension.pkgName)) }) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(R.string.settings),
        )
    }
}
// <-- AM (BR)

@Composable
fun AnimeSourceOptionsDialog(
    source: AnimeSource,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    // AM (BR) -->
    onClickUpdate: () -> Unit,
    onClickUninstall: () -> Unit,
    // <-- AM (BR)
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = source.visualName)
        },
        text = {
            Column {
                val textId = if (Pin.Pinned in source.pin) R.string.action_unpin else R.string.action_pin
                Text(
                    text = stringResource(textId),
                    modifier = Modifier
                        .clickable(onClick = onClickPin)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
                if (source.id != LocalAnimeSource.ID) {
                    Text(
                        text = stringResource(id = R.string.action_disable),
                        modifier = Modifier
                            .clickable(onClick = onClickDisable)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                    // AM (BR) -->
                    if (source.installedExtension.hasUpdate) {
                        Text(
                            text = stringResource(id = R.string.ext_update),
                            modifier = Modifier
                                .clickable(onClick = onClickUpdate)
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.ext_uninstall),
                        modifier = Modifier
                            .clickable(onClick = onClickUninstall)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                    // <-- AM (BR)
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {},
    )
}

sealed class AnimeSourceUiModel {
    data class Item(val source: AnimeSource) : AnimeSourceUiModel()
    data class Header(val language: String) : AnimeSourceUiModel()
}
