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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.source.anime.model.installedExtension
import eu.kanade.presentation.browse.anime.AnimeSourceOptionsDialog
import eu.kanade.presentation.browse.anime.AnimeSourcesScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connection.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connection.discord.DiscordScreen
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.ui.browse.anime.source.AnimeSourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreen
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.core.util.lang.launchIO
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class BrowseTab(
    private val toExtensions: Boolean = false,
) : Tab() {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current is BrowseTab
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 2u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalAnimeSearchScreen())
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        // AM (BROWSE) -->
        val snackbarHostState = SnackbarHostState()
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AnimeSourcesScreenModel() }
        val state by screenModel.state.collectAsState()
        val updateCount by screenModel.sourcePreferences.animeExtensionUpdatesCount().collectAsState()

        AnimeSourcesScreen(
            state = state,
            navigator = navigator,
            updateCount = updateCount,
            onClickItem = { source, listing ->
                navigator.push(BrowseAnimeSourceScreen(source.id, listing.query))
            },
            onClickPin = screenModel::togglePin,
            onLongClickItem = screenModel::showSourceDialog,
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
                onClickUninstall = {
                    screenModel.uninstallExtension(source.installedExtension)
                    screenModel.closeDialog()
                },
                onDismiss = screenModel::closeDialog,
            )
        }

        val internalErrString = stringResource(MR.strings.internal_error)
        LaunchedEffect(Unit) {
            // AM (DISCORD_RPC) -->
            with(DiscordRPCService) {
                discordScope.launchIO { setScreen(context.applicationContext, DiscordScreen.BROWSE) }
            }
            // <-- AM (DISCORD_RPC)
            (context as? MainActivity)?.ready = true
            launchIO {
                Injekt.get<AnimeExtensionManager>().findAvailableExtensions()
                screenModel.events.collectLatest { event ->
                    when (event) {
                        AnimeSourcesScreenModel.Event.FailedFetchingSources -> {
                            launch { snackbarHostState.showSnackbar(internalErrString) }
                        }
                    }
                }
            }
        }
        // <-- AM (BROWSE)
    }
}
