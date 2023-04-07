package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.browse.anime.AnimeSourceOptionsDialog
import eu.kanade.presentation.browse.anime.AnimeSourcesScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.ui.browse.anime.source.AnimeSourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class BrowseTab(
    // AM (BR) -->
    private val extHasUpdate: Boolean,
    // <-- AM (BR)
    private val toExtensions: Boolean = false,
) : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(R.string.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        // AM (BR) -->
        val snackbarHostState = SnackbarHostState()
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AnimeSourcesScreenModel() }
        val state by screenModel.state.collectAsState()

        AnimeSourcesScreen(
            state = state,
            navigator = navigator,
            onClickItem = { source, listing ->
                screenModel.onOpenSource(source)
                navigator.push(BrowseAnimeSourceScreen(source.id, listing.query))
            },
            onClickPin = screenModel::togglePin,
            onLongClickItem = screenModel::showSourceDialog,
            extHasUpdate = extHasUpdate,
        )

        state.dialog?.let { dialog ->
            val source = dialog.source
            AnimeSourceOptionsDialog(
                source = source,
                onClickPin = {
                    screenModel.togglePin(source)
                    screenModel.closeDialog()
                },
                onClickDisable = {
                    screenModel.toggleSource(source)
                    screenModel.closeDialog()
                },
                onClickUpdate = {
                    screenModel.updateExtension(source.installedExtension)
                    screenModel.closeDialog()
                },
                onClickUninstall = {
                    screenModel.uninstallExtension(source.installedExtension.pkgName)
                    screenModel.closeDialog()
                },
                onDismiss = screenModel::closeDialog,
            )
        }
        // <-- AM (BR)

        // For local source
        DiskUtil.RequestStoragePermission()

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
            // AM (DC) -->
            DiscordRPCService.setDiscordPage(3)
            // <-- AM (DC)
        }

        // AM (BR) -->
        val internalErrString = stringResource(R.string.internal_error)
        LaunchedEffect(Unit) {
            Injekt.get<AnimeExtensionManager>().updatePendingUpdatesCount()
            screenModel.events.collectLatest { event ->
                when (event) {
                    AnimeSourcesScreenModel.Event.FailedFetchingSources -> {
                        launch { snackbarHostState.showSnackbar(internalErrString) }
                    }
                }
            }
        }
        // <-- AM (BR)
    }
}
