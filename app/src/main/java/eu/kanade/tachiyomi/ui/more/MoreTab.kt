package eu.kanade.tachiyomi.ui.more

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.core.preference.asState
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadService
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.download.AnimeDownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import eu.kanade.tachiyomi.util.system.isInstalledFromFDroid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import tachiyomi.core.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

object MoreTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_more_enter)
            return TabOptions(
                index = 4u,
                title = stringResource(R.string.label_more),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(SettingsScreen.toMainScreen())
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MoreScreenModel() }
        val downloadQueueState by screenModel.downloadQueueState.collectAsState()
        val playerPreferences: PlayerPreferences by injectLazy()

        MoreScreen(
            downloadQueueStateProvider = { downloadQueueState },
            downloadedOnly = screenModel.downloadedOnly,
            onDownloadedOnlyChange = { screenModel.downloadedOnly = it },
            incognitoMode = screenModel.incognitoMode,
            onIncognitoModeChange = { screenModel.incognitoMode = it },
            isFDroid = context.isInstalledFromFDroid(),
            // AM (UH) -->
            onClickUpdates = {
                // AM (DC) -->
                DiscordRPCService.setDiscordPage(1)
                // <-- AM (DC)
                navigator.push(
                    UpdatesTab(
                        fromMore = true,
                        externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
                    ),
                )
            },
            onClickHistory = {
                // AM (DC) -->
                DiscordRPCService.setDiscordPage(2)
                // <-- AM (DC)
                navigator.push(
                    HistoryTab(
                        fromMore = false,
                        externalPlayer = playerPreferences.alwaysUseExternalPlayer().get(),
                    ),
                )
            },
            // <-- AM (UH)
            onClickDownloadQueue = { navigator.push(AnimeDownloadQueueScreen) },
            onClickCategories = { navigator.push(CategoryScreen()) },
            onClickStats = { navigator.push(StatsScreen()) },
            onClickBackupAndRestore = { navigator.push(SettingsScreen.toBackupScreen()) },
            onClickSettings = { navigator.push(SettingsScreen.toMainScreen()) },
            onClickAbout = { navigator.push(SettingsScreen.toAboutScreen()) },
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
            // AM (DC) -->
            DiscordRPCService.setDiscordPage(4)
            // <-- AM (DC)
        }
    }
}

private class MoreScreenModel(
    private val animeDownloadManager: AnimeDownloadManager = Injekt.get(),
    preferences: BasePreferences = Injekt.get(),
) : ScreenModel {

    var downloadedOnly by preferences.downloadedOnly().asState(coroutineScope)
    var incognitoMode by preferences.incognitoMode().asState(coroutineScope)

    private var _state: MutableStateFlow<DownloadQueueState> = MutableStateFlow(DownloadQueueState.Stopped)
    val downloadQueueState: StateFlow<DownloadQueueState> = _state.asStateFlow()

    init {
        // Handle running/paused status change and queue progress updating
        coroutineScope.launchIO {
            combine(
                AnimeDownloadService.isRunning,
                animeDownloadManager.queueState,
            ) { isRunningAnime, animeDownloadQueue -> Pair(isRunningAnime, animeDownloadQueue.size) }
                .collectLatest { (isDownloadingAnime, animeDownloadQueueSize) ->
                    val isDownloading = isDownloadingAnime
                    val downloadQueueSize = animeDownloadQueueSize
                    val pendingDownloadExists = downloadQueueSize != 0
                    _state.value = when {
                        !pendingDownloadExists -> DownloadQueueState.Stopped
                        !isDownloading && !pendingDownloadExists -> DownloadQueueState.Paused(0)
                        !isDownloading && pendingDownloadExists -> DownloadQueueState.Paused(downloadQueueSize)
                        else -> DownloadQueueState.Downloading(downloadQueueSize)
                    }
                }
        }
    }
}

sealed class DownloadQueueState {
    object Stopped : DownloadQueueState()
    data class Paused(val pending: Int) : DownloadQueueState()
    data class Downloading(val pending: Int) : DownloadQueueState()
}
