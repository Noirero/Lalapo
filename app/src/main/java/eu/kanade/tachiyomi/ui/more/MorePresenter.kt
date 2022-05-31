package eu.kanade.tachiyomi.ui.more

import android.os.Bundle
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.AnimeDownloadService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MorePresenter(
    private val animedownloadManager: AnimeDownloadManager = Injekt.get(),
    preferencesHelper: PreferencesHelper = Injekt.get(),
) : BasePresenter<MoreController>() {

    val downloadedOnly = preferencesHelper.downloadedOnly().asState()
    val incognitoMode = preferencesHelper.incognitoMode().asState()

    val showNavUpdates = preferencesHelper.showNavUpdates().asState()
    val showNavHistory = preferencesHelper.showNavHistory().asState()

    private var _state: MutableStateFlow<DownloadQueueState> = MutableStateFlow(DownloadQueueState.Stopped)
    val downloadQueueState: StateFlow<DownloadQueueState> = _state.asStateFlow()

    private var isDownloadingAnime: Boolean = false
    private var downloadQueueSizeAnime: Int = 0
    private var untilDestroySubscriptions = CompositeSubscription()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (untilDestroySubscriptions.isUnsubscribed) {
            untilDestroySubscriptions = CompositeSubscription()
        }

        initDownloadQueueSummary()
    }

    override fun onDestroy() {
        super.onDestroy()
        untilDestroySubscriptions.unsubscribe()
    }

    private fun initDownloadQueueSummary() {
        // Handle running/paused status change

        AnimeDownloadService.runningRelay
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { isRunning ->
                isDownloadingAnime = isRunning
                updateDownloadQueueState()
            }

        animedownloadManager.queue.getUpdatedObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy {
                downloadQueueSizeAnime = it.size
                updateDownloadQueueState()
            }
    }

    private fun updateDownloadQueueState() {
        presenterScope.launchIO {
            val pendingDownloadExists = downloadQueueSizeAnime != 0
            _state.value = when {
                !pendingDownloadExists -> DownloadQueueState.Stopped
                !isDownloadingAnime && !pendingDownloadExists -> DownloadQueueState.Paused(0)
                !isDownloadingAnime && pendingDownloadExists -> DownloadQueueState.Paused(downloadQueueSizeAnime)
                else -> DownloadQueueState.Downloading(downloadQueueSizeAnime)
            }
        }
    }

    private fun <T> Observable<T>.subscribeUntilDestroy(onNext: (T) -> Unit): Subscription {
        return subscribe(onNext).also { untilDestroySubscriptions.add(it) }
    }
}

sealed class DownloadQueueState {
    object Stopped : DownloadQueueState()
    data class Paused(val pending: Int) : DownloadQueueState()
    data class Downloading(val pending: Int) : DownloadQueueState()
}
