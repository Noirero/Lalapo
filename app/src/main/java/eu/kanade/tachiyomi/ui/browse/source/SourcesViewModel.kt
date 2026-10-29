package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.SourceUiModel
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import java.util.TreeMap
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class SourcesViewModel(
    private val getEnabledSources: GetEnabledSources,
    private val toggleSource: ToggleSource,
    private val toggleSourcePin: ToggleSourcePin,
    // AM (BROWSE) -->
    private val extensionManager: ExtensionManager,
    internal val sourcePreferences: SourcePreferences,
    // <-- AM (BROWSE)
) : ViewModel() {

    private val dialog = MutableStateFlow<Dialog?>(null)
    private val sourceRetry = MutableStateFlow(0L)

    private val sourceLoadState = sourceRetry.flatMapLatest {
        getEnabledSources.subscribe()
            .map<List<Source>, SourceLoadState> { sources ->
                SourceLoadState.Success(toSourceUiModels(sources))
            }
            .onStart { emit(SourceLoadState.Loading) }
            .catch {
                logcat(LogPriority.ERROR, it)
                emit(SourceLoadState.Error)
            }
    }

    val state: StateFlow<State> = combine(
        sourceLoadState,
        dialog,
    ) { loadState, dialog ->
        when (loadState) {
            SourceLoadState.Loading -> State(dialog = dialog, isLoading = true)
            SourceLoadState.Error -> State(dialog = dialog, isLoading = false, hasError = true)
            is SourceLoadState.Success -> State(dialog = dialog, isLoading = false, items = loadState.items)
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), State())

    fun retrySources() {
        sourceRetry.update { it + 1 }
    }

    private fun toSourceUiModels(sources: List<Source>): List<SourceUiModel> {
        val map = TreeMap<String, MutableList<Source>> { d1, d2 ->
            // Sources without a lang defined will be placed at the end
            when {
                d1 == LAST_USED_KEY && d2 != LAST_USED_KEY -> -1
                d2 == LAST_USED_KEY && d1 != LAST_USED_KEY -> 1
                d1 == PINNED_KEY && d2 != PINNED_KEY -> -1
                d2 == PINNED_KEY && d1 != PINNED_KEY -> 1
                d1 == "" && d2 != "" -> 1
                d2 == "" && d1 != "" -> -1
                else -> d1.compareTo(d2)
            }
        }
        val byLang = sources.groupByTo(map) {
            when {
                it.isUsedLast -> LAST_USED_KEY
                Pin.Actual in it.pin -> PINNED_KEY
                else -> it.lang
            }
        }

        return byLang.flatMap {
            listOf(
                SourceUiModel.Header(it.key),
                *it.value.map { source ->
                    SourceUiModel.Item(
                        source,
                        // AM (BROWSE) -->
                        extensionManager.getInstalledExtension(source.id),
                        // <-- AM (BROWSE)
                    )
                }.toTypedArray(),
            )
        }
    }

    fun toggleSource(source: Source) {
        toggleSource.await(source)
    }

    fun togglePin(source: Source) {
        toggleSourcePin.await(source)
    }

    fun showSourceDialog(source: Source) {
        dialog.update {
            Dialog(
                source,
                // AM (BROWSE) -->
                extensionManager.getInstalledExtension(source.id),
                // <-- AM (BROWSE)
            )
        }
    }

    fun closeDialog() {
        dialog.update { null }
    }

    // AM (BROWSE) -->
    fun uninstallExtension(extension: Extension.Installed) {
        viewModelScope.launchIO {
            extensionManager.uninstallExtension(extension)
        }
    }
    // <-- AM (BROWSE)

    private sealed interface SourceLoadState {
        data object Loading : SourceLoadState
        data object Error : SourceLoadState
        data class Success(val items: List<SourceUiModel>) : SourceLoadState
    }

    data class Dialog(
        val source: Source,
        // AM (BROWSE) -->
        val extension: Extension.Installed?,
        // <-- AM (BROWSE)
    )

    @Immutable
    data class State(
        val dialog: Dialog? = null,
        val isLoading: Boolean = true,
        val hasError: Boolean = false,
        val items: List<SourceUiModel> = listOf(),
    ) {
        val isEmpty = !isLoading && !hasError && items.isEmpty()
    }

    companion object {
        const val PINNED_KEY = "pinned"
        const val LAST_USED_KEY = "last_used"
    }
}
