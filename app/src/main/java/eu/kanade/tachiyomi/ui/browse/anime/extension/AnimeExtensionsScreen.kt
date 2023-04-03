// AM (BR) -->
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

class AnimeExtensionsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AnimeExtensionsScreenModel() }
        val state by screenModel.state.collectAsState()
        val searchQuery by screenModel.query.collectAsState()
        val onChangeSearchQuery = screenModel::search

        AnimeExtensionScreen(
            state = state,
            navigator = navigator,
            searchQuery = searchQuery,
            onLongClickItem = { extension ->
                when (extension) {
                    is AnimeExtension.Available -> screenModel.installExtension(extension)
                    else -> screenModel.uninstallExtension(extension.pkgName)
                }
            },
            onChangeSearchQuery = onChangeSearchQuery,
            onClickItemCancel = screenModel::cancelInstallUpdateExtension,
            onClickUpdateAll = screenModel::updateAllExtensions,
            onInstallExtension = screenModel::installExtension,
            onOpenExtension = { navigator.push(AnimeExtensionDetailsScreen(it.pkgName)) },
            onTrustExtension = { screenModel.trustSignature(it.signatureHash) },
            onUninstallExtension = { screenModel.uninstallExtension(it.pkgName) },
            onUpdateExtension = screenModel::updateExtension,
            onRefresh = screenModel::findAvailableExtensions,
        )
    }
}
// <-- AM (BR)
