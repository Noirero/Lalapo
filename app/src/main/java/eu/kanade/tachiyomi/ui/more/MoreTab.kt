package eu.kanade.tachiyomi.ui.more

import android.content.Context
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.core.preference.asState
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.connection.SyncPreferences
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.presentation.more.ExtensionHealthState
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.presentation.more.StorageHealthState
import eu.kanade.presentation.more.SyncHealthState
import eu.kanade.presentation.more.SyncProvider
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connection.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connection.discord.DiscordScreen
import eu.kanade.tachiyomi.data.connection.syncmiru.SyncDataJob
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import eu.kanade.tachiyomi.ui.storage.StorageScreen
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object MoreTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_more_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.label_more),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(SettingsScreen())
    }

    override suspend fun onReselectHold(navigator: Navigator) {
        navigator.push(StorageScreen())
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val viewModel = metroViewModel<MoreViewModel>()

        val downloadQueueState by viewModel.downloadQueueState.collectAsState()
        val syncHealth by viewModel.syncHealth.collectAsState()
        val extensionHealth by viewModel.extensionHealth.collectAsState()
        val storageHealth by viewModel.storageHealth.collectAsState()

        MoreScreen(
            downloadQueueStateProvider = { downloadQueueState },
            downloadedOnly = viewModel.downloadedOnly,
            onDownloadedOnlyChange = { viewModel.downloadedOnly = it },
            incognitoMode = viewModel.incognitoMode,
            onIncognitoModeChange = { viewModel.incognitoMode = it },
            syncHealth = syncHealth,
            extensionHealth = extensionHealth,
            storageHealth = storageHealth,
            onClickDownloadQueue = { navigator.push(DownloadQueueScreen) },
            onClickSyncAndBackup = { navigator.push(SettingsScreen(SettingsScreen.Destination.SyncAndBackup)) },
            onSyncNow = viewModel::syncNow,
            onClickExtensionHealth = {
                BrowseTab.showExtension()
                tabNavigator.current = BrowseTab
            },
            onClickStorageHealth = {
                navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage))
            },
            onClickStats = { navigator.push(StatsScreen()) },
            onClickSettings = { navigator.push(SettingsScreen()) },
            onClickAbout = { navigator.push(SettingsScreen(SettingsScreen.Destination.About)) },
        )

        LaunchedEffect(Unit) {
            with(DiscordRPCService) {
                discordScope.launchIO { setScreen(context.applicationContext, DiscordScreen.MORE) }
            }
            viewModel.refreshContextualHealth()
        }
    }
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class MoreViewModel(
    private val context: Context,
    private val downloadManager: DownloadManager,
    private val syncPreferences: SyncPreferences,
    private val getExtensions: GetExtensionsByType,
    private val storageManager: StorageManager,
    preferences: BasePreferences,
) : ViewModel() {

    var downloadedOnly by preferences.downloadedOnly.asState(viewModelScope)
    var incognitoMode by preferences.incognitoMode.asState(viewModelScope)

    private val _downloadQueueState: MutableStateFlow<DownloadQueueState> = MutableStateFlow(DownloadQueueState.Stopped)
    val downloadQueueState: StateFlow<DownloadQueueState> = _downloadQueueState.asStateFlow()

    private val syncingState = MutableStateFlow(false)
    private var syncWatchJob: Job? = null

    private val syncApiKey = syncPreferences.clientAPIKey.stateIn(viewModelScope)
    private val syncDriveToken = syncPreferences.googleDriveRefreshToken.stateIn(viewModelScope)
    private val lastSyncTimestamp = syncPreferences.lastSyncTimestamp.stateIn(viewModelScope)
    private val lastSyncError = syncPreferences.lastSyncError.stateIn(viewModelScope)

    val syncHealth: StateFlow<SyncHealthState> = combine(
        syncApiKey,
        syncDriveToken,
        lastSyncTimestamp,
        lastSyncError,
        syncingState,
    ) { apiKey, driveToken, timestamp, error, isSyncing ->
        val provider = when {
            apiKey.isNotBlank() && driveToken.isNotBlank() -> SyncProvider.Multiple
            driveToken.isNotBlank() -> SyncProvider.GoogleDrive
            apiKey.isNotBlank() -> SyncProvider.SyncYomi
            else -> SyncProvider.None
        }
        SyncHealthState(
            enabled = provider != SyncProvider.None,
            provider = provider,
            lastSyncTimestamp = timestamp,
            lastError = error.ifBlank { null },
            isSyncing = isSyncing,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        syncHealthSnapshot(),
    )

    val extensionHealth: StateFlow<ExtensionHealthState> = getExtensions.subscribe()
        .map { extensions ->
            ExtensionHealthState(
                updates = extensions.updates.size,
                untrusted = extensions.untrusted.size,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ExtensionHealthState(),
        )

    private val _storageHealth = MutableStateFlow(StorageHealthState())
    val storageHealth: StateFlow<StorageHealthState> = _storageHealth.asStateFlow()

    init {
        viewModelScope.launchIO {
            combine(
                downloadManager.isDownloaderRunning,
                downloadManager.queueState,
            ) { isRunning, downloadQueue -> Pair(isRunning, downloadQueue.size) }
                .collectLatest { (isDownloading, downloadQueueSize) ->
                    val pendingDownloadExists = downloadQueueSize != 0
                    _downloadQueueState.value = when {
                        !pendingDownloadExists -> DownloadQueueState.Stopped
                        !isDownloading -> DownloadQueueState.Paused(downloadQueueSize)
                        else -> DownloadQueueState.Downloading(downloadQueueSize)
                    }
                }
        }

        viewModelScope.launchIO {
            storageManager.changes.collectLatest {
                refreshStorageHealth()
            }
        }

        refreshContextualHealth()
    }

    fun refreshContextualHealth() {
        refreshStorageHealth()
        watchSyncState()
    }

    fun syncNow() {
        if (!syncPreferences.isSyncEnabled()) return
        SyncDataJob.startNow(context.workManager)
        watchSyncState(assumeStarting = true)
    }

    private fun watchSyncState(assumeStarting: Boolean = false) {
        if (syncWatchJob?.isActive == true) return
        syncWatchJob = viewModelScope.launchIO {
            if (assumeStarting) {
                syncingState.value = true
                delay(750)
            }

            var running = SyncDataJob.isRunning(context.workManager)
            syncingState.value = running || assumeStarting
            while (running) {
                delay(1_000)
                running = SyncDataJob.isRunning(context.workManager)
                syncingState.value = running
            }
            syncingState.value = false
        }
    }

    private fun refreshStorageHealth() {
        viewModelScope.launchIO {
            val available = storageManager.getDownloadsDirectory()
                ?.let(DiskUtil::getAvailableStorageSpace)
                ?.takeIf { it >= 0L }
                ?: DiskUtil.getExternalStorages(context)
                    .firstOrNull()
                    ?.let(DiskUtil::getAvailableStorageSpace)
                ?: -1L

            _storageHealth.value = StorageHealthState(
                isLow = available in 0 until LOW_STORAGE_THRESHOLD_BYTES,
                availableBytes = available,
            )
        }
    }

    private fun syncHealthSnapshot(): SyncHealthState {
        val apiKey = syncPreferences.clientAPIKey.get()
        val driveToken = syncPreferences.googleDriveRefreshToken.get()
        val provider = when {
            apiKey.isNotBlank() && driveToken.isNotBlank() -> SyncProvider.Multiple
            driveToken.isNotBlank() -> SyncProvider.GoogleDrive
            apiKey.isNotBlank() -> SyncProvider.SyncYomi
            else -> SyncProvider.None
        }
        return SyncHealthState(
            enabled = provider != SyncProvider.None,
            provider = provider,
            lastSyncTimestamp = syncPreferences.lastSyncTimestamp.get(),
            lastError = syncPreferences.lastSyncError.get().ifBlank { null },
            isSyncing = false,
        )
    }

    companion object {
        private const val LOW_STORAGE_THRESHOLD_BYTES = 512L * 1024L * 1024L
    }
}

sealed interface DownloadQueueState {
    data object Stopped : DownloadQueueState
    data class Paused(val pending: Int) : DownloadQueueState
    data class Downloading(val pending: Int) : DownloadQueueState
}
