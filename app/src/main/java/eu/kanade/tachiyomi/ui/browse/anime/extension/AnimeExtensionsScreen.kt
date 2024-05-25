// AM (BROWSE) -->
package eu.kanade.tachiyomi.ui.browse.anime.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.anime.AnimeExtensionScreen
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.ui.browse.anime.extension.details.AnimeExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen

class AnimeExtensionsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AnimeExtensionsScreenModel() }
        val state by screenModel.state.collectAsState()

        AnimeExtensionScreen(
            state = state,
            navigator = navigator,
            searchQuery = state.searchQuery,
            onLongClickItem = { extension ->
                when (extension) {
                    is AnimeExtension.Available -> screenModel.installExtension(extension)
                    else -> screenModel.uninstallExtension(extension)
                }
            },
            onChangeSearchQuery = screenModel::search,
            onClickItemCancel = screenModel::cancelInstallUpdateExtension,
            onClickUpdateAll = screenModel::updateAllExtensions,
            onOpenWebView = { extension ->
                extension.sources.getOrNull(0)?.let {
                    navigator.push(WebViewScreen(url = it.baseUrl, initialTitle = it.name, sourceId = it.id))
                }
            },
            onInstallExtension = screenModel::installExtension,
            onOpenExtension = { navigator.push(AnimeExtensionDetailsScreen(it.pkgName)) },
            onTrustExtension = screenModel::trustExtension,
            onUninstallExtension = screenModel::uninstallExtension,
            onUpdateExtension = screenModel::updateExtension,
            onRefresh = screenModel::findAvailableExtensions,
        )
    }
}
// <-- AM (BROWSE)
