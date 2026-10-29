package eu.kanade.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrateSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.SourcesFilterScreen
import eu.kanade.tachiyomi.ui.browse.source.SourcesViewModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel.Listing
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.isLocal

@Composable
fun SourcesScreen(
    state: SourcesViewModel.State,
    onClickItem: (Source, Listing) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onRetry: () -> Unit,
    // AM (BROWSE) -->
    toExtensionsScreen: () -> Unit,
    updateCount: Int,
    modifier: Modifier = Modifier,
    // <-- AM (BROWSE)
) {
    // AM (BROWSE) -->
    val navigator = LocalNavigator.currentOrThrow
    var showToolsMenu by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = { scrollBehavior ->
            AppBar(
                titleContent = { AppBarTitle(stringResource(AMMR.strings.am_label_discover)) },
                actions = {
                    IconButton(onClick = toExtensionsScreen) {
                        BadgedBox(
                            badge = {
                                if (updateCount > 0) {
                                    Badge { Text(updateCount.toString()) }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_extension_24dp),
                                contentDescription = stringResource(MR.strings.label_extensions),
                            )
                        }
                    }
                    IconButton(onClick = { navigator.push(SourcesFilterScreen()) }) {
                        Icon(
                            Icons.Outlined.FilterList,
                            contentDescription = stringResource(MR.strings.action_filter),
                        )
                    }
                    Box {
                        IconButton(onClick = { showToolsMenu = true }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = stringResource(MR.strings.label_more),
                            )
                        }
                        DropdownMenu(
                            expanded = showToolsMenu,
                            onDismissRequest = { showToolsMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(MR.strings.action_migrate)) },
                                onClick = {
                                    showToolsMenu = false
                                    navigator.push(MigrateSourceScreen())
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        val extensionsListState = rememberLazyListState()
        // <-- AM (BROWSE)
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            state.hasError -> EmptyScreen(
                stringRes = MR.strings.internal_error,
                modifier = Modifier.padding(contentPadding),
                actions = listOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.action_retry,
                        icon = Icons.Outlined.Refresh,
                        onClick = onRetry,
                    ),
                ),
            )
            state.isEmpty -> EmptyScreen(
                stringRes = MR.strings.source_empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            else -> {
                ScrollbarLazyColumn(
                    state = extensionsListState,
                    contentPadding = contentPadding + topSmallPaddingValues,
                ) {
                    item(key = "discover-search") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navigator.push(GlobalSearchScreen()) }
                                .padding(
                                    horizontal = MaterialTheme.padding.medium,
                                    vertical = MaterialTheme.padding.small,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = stringResource(MR.strings.action_global_search_hint),
                                modifier = Modifier.padding(start = MaterialTheme.padding.medium),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(
                        items = state.items,
                        contentType = {
                            when (it) {
                                is SourceUiModel.Header -> "header"
                                is SourceUiModel.Item -> "item"
                            }
                        },
                        key = {
                            when (it) {
                                is SourceUiModel.Header -> it.hashCode()
                                is SourceUiModel.Item -> "source-${it.source.key()}"
                            }
                        },
                    ) { model ->
                        when (model) {
                            is SourceUiModel.Header -> {
                                SourceHeader(
                                    modifier = Modifier.animateItem(),
                                    language = model.language,
                                )
                            }
                            is SourceUiModel.Item -> SourceItem(
                                modifier = Modifier.animateItem(),
                                source = model.source,
                                onClickItem = onClickItem,
                                onLongClickItem = onLongClickItem,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceHeader(
    language: String,
    modifier: Modifier = Modifier,
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
private fun SourceItem(
    source: Source,
    onClickItem: (Source, Listing) -> Unit,
    onLongClickItem: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        onClickItem = { onClickItem(source, Listing.Popular) },
        onLongClickItem = { onLongClickItem(source) },
        action = {
            IconButton(onClick = { onLongClickItem(source) }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(MR.strings.label_more),
                )
            }
        },
        pin = {
            if (Pin.Pinned in source.pin) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                    contentDescription = stringResource(MR.strings.action_unpin),
                )
            }
        },
    )
}

@Composable
fun SourceOptionsDialog(
    source: Source,
    onClickLatest: (() -> Unit)?,
    onClickPin: () -> Unit,
    onClickSettings: (() -> Unit)?,
    onClickDisable: () -> Unit,
    // AM (BROWSE) -->
    onClickUninstall: (() -> Unit)?,
    // <-- AM (BROWSE)
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = source.visualName)
        },
        text = {
            Column {
                onClickLatest?.let { latest ->
                    Text(
                        text = stringResource(MR.strings.latest),
                        modifier = Modifier
                            .clickable(onClick = latest)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
                val textId = if (Pin.Pinned in source.pin) MR.strings.action_unpin else MR.strings.action_pin
                Text(
                    text = stringResource(textId),
                    modifier = Modifier
                        .clickable(onClick = onClickPin)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
                onClickSettings?.let { settings ->
                    Text(
                        text = stringResource(MR.strings.label_settings),
                        modifier = Modifier
                            .clickable(onClick = settings)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
                if (!source.isLocal()) {
                    Text(
                        text = stringResource(MR.strings.action_disable),
                        modifier = Modifier
                            .clickable(onClick = onClickDisable)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
                // AM (BROWSE) -->
                onClickUninstall?.let { uninstall ->
                    Text(
                        text = stringResource(resource = MR.strings.ext_uninstall),
                        modifier = Modifier
                            .clickable(onClick = uninstall)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
                // <-- AM (BROWSE)
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {},
    )
}

sealed interface SourceUiModel {
    data class Item(val source: Source) : SourceUiModel
    data class Header(val language: String) : SourceUiModel
}
