package eu.kanade.tachiyomi.ui.category

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.icerock.moko.resources.StringResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.DeleteCategory
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.GetVisibleCategories
import tachiyomi.domain.category.interactor.HideCategory
import tachiyomi.domain.category.interactor.RenameCategory
import tachiyomi.domain.category.interactor.ReorderCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategorySetRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class CategoryViewModel(
    private val getCategories: GetCategories,
    private val categorySetRepository: CategorySetRepository,
    private val createCategoryWithName: CreateCategoryWithName,
    private val deleteCategory: DeleteCategory,
    private val reorderCategory: ReorderCategory,
    private val renameCategory: RenameCategory,
    // AY -->
    private val getVisibleCategories: GetVisibleCategories,
    private val hideCategory: HideCategory,
    private val libraryPreferences: LibraryPreferences,
    // <-- AY
) : ViewModel() {

    val categorySets = categorySetRepository.observeSets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    val activeSetKey = libraryPreferences.activeCategorySetKey.changes()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5.seconds),
            libraryPreferences.activeCategorySetKey.get(),
        )

    // Management is backed by SQLDelight flows, including hidden categories.
    val categoriesBySet: StateFlow<Map<Long, List<Category>>?> = categorySetRepository.observeCategoriesBySet()
        .map<Map<Long, List<Category>>, Map<Long, List<Category>>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    private val _setBusy = MutableStateFlow(false)
    val setBusy: StateFlow<Boolean> = _setBusy

    private val _setError = MutableStateFlow<String?>(null)
    val setError: StateFlow<String?> = _setError

    fun clearSetError() {
        _setError.value = null
    }

    fun createSet(name: String, onSuccess: () -> Unit = {}) = runSetOperation(onSuccess) {
        categorySetRepository.create(name)
    }

    fun renameSet(id: Long, name: String, onSuccess: () -> Unit = {}) = runSetOperation(onSuccess) {
        categorySetRepository.rename(id, name)
    }

    fun selectSet(key: String) = runSetOperation {
        categorySetRepository.selectActiveSet(key)
    }

    fun deleteSet(id: Long, destinationId: Long?, onSuccess: () -> Unit = {}) = runSetOperation(onSuccess) {
        categorySetRepository.delete(id, destinationId)
    }

    fun moveCategory(categoryId: Long, destinationId: Long, onSuccess: () -> Unit = {}) =
        runSetOperation(onSuccess) {
            categorySetRepository.moveCategories(listOf(categoryId), destinationId)
        }

    fun createCategoryInSet(name: String, setId: Long, onSuccess: () -> Unit = {}) =
        runSetOperation(onSuccess) {
            when (val result = createCategoryWithName.await(name, setId)) {
                is CreateCategoryWithName.Result.InternalError -> throw result.error
                CreateCategoryWithName.Result.Success -> Unit
            }
        }

    fun toggleManagedCategoryVisibility(category: Category) = runSetOperation {
        when (val result = hideCategory.await(category)) {
            is HideCategory.Result.InternalError -> throw result.error
            HideCategory.Result.Success -> Unit
        }
    }

    fun reorderSets(ids: List<Long>) = runSetOperation {
        categorySetRepository.reorder(ids)
    }

    // Claim the mutation synchronously, before launching a coroutine, to close
    // the double-tap window while the UI awaits the next StateFlow emission.
    private fun runSetOperation(onSuccess: () -> Unit = {}, operation: suspend () -> Unit) {
        if (_setBusy.value) return
        _setBusy.value = true
        _setError.value = null
        viewModelScope.launch {
            try {
                operation()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _setError.value = when (e.message) {
                    null, "" -> "Operasi Set gagal. Silakan coba lagi."
                    "Duplicate Set name" -> "Nama Set sudah digunakan."
                    "Category name is required" -> "Nama kategori tidak boleh kosong."
                    "Unknown target Set" -> "Set tujuan tidak ditemukan."
                    "Destination Set is required" -> "Pilih Set tujuan terlebih dahulu."
                    "Invalid destination Set" -> "Set tujuan tidak valid."
                    "Cannot delete last or unknown Set" -> "Set terakhir tidak dapat dihapus."
                    else -> e.message
                }
            } finally {
                _setBusy.value = false
            }
        }
    }

    private val _events: Channel<CategoryEvent> = Channel()
    val events = _events.receiveAsFlow()

    private val dialog = MutableStateFlow<CategoryDialog?>(null)

    val state: StateFlow<CategoryScreenState> = combine(
        // AY -->
        libraryPreferences.hideHiddenCategoriesSettings.changes(),
        getVisibleCategories.subscribe(),
        // <-- AY
        getCategories.subscribe(),
        dialog,
    ) { hideHidden, visible, categories, dialog ->
        CategoryScreenState.Success(
            categories = (if (hideHidden) visible else categories).filterNot(Category::isSystemCategory),
            dialog = dialog,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), CategoryScreenState.Loading)

    fun createCategory(name: String) {
        viewModelScope.launch {
            when (createCategoryWithName.await(name)) {
                is CreateCategoryWithName.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    // AY -->
    fun hideCategory(category: Category) {
        viewModelScope.launch {
            when (hideCategory.await(category)) {
                is HideCategory.Result.InternalError -> _events.send(
                    CategoryEvent.InternalError,
                )
                else -> {}
            }
        }
    }
    // <-- AY

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            when (deleteCategory.await(categoryId = categoryId)) {
                is DeleteCategory.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun changeOrder(category: Category, newIndex: Int) {
        viewModelScope.launch {
            when (reorderCategory.await(category, newIndex)) {
                is ReorderCategory.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun renameCategory(category: Category, name: String) {
        viewModelScope.launch {
            when (renameCategory.await(category, name)) {
                is RenameCategory.Result.InternalError -> _events.send(CategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun showDialog(dialog: CategoryDialog) {
        this.dialog.update { dialog }
    }

    fun dismissDialog() {
        dialog.update { null }
    }
}

sealed interface CategoryDialog {
    data object Create : CategoryDialog
    data class Rename(val category: Category) : CategoryDialog
    data class Delete(val category: Category) : CategoryDialog
}

sealed interface CategoryEvent {
    sealed class LocalizedMessage(val stringRes: StringResource) : CategoryEvent
    data object InternalError : LocalizedMessage(MR.strings.internal_error)
}

sealed interface CategoryScreenState {

    @Immutable
    data object Loading : CategoryScreenState

    @Immutable
    data class Success(
        val categories: List<Category>,
        val dialog: CategoryDialog? = null,
    ) : CategoryScreenState {

        val isEmpty: Boolean
            get() = categories.isEmpty()
    }
}
