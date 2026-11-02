package eu.kanade.tachiyomi.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategorySetRepository
import tachiyomi.domain.library.service.LibraryPreferences

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class CategoryPickerViewModel(
    private val categorySets: CategorySetRepository,
    private val preferences: LibraryPreferences,
    private val categories: GetCategories,
    private val createCategory: CreateCategoryWithName,
    private val setAnimeCategories: SetAnimeCategories,
) : ViewModel() {
    val sets = categorySets.observeSets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Null distinguishes an uninitialized DB flow from a genuinely empty Set.
    val categoriesBySet = categorySets.observeCategoriesBySet()
        .map<Map<Long, List<Category>>, Map<Long, List<Category>>?> {
            it
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeKey = preferences.activeCategorySetKey.changes()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            preferences.activeCategorySetKey.get(),
        )

    /** Initial membership is a snapshot, not a write buffer. Includes hidden categories. */
    suspend fun membershipCounts(animeIds: List<Long>): Map<Long, Int> {
        val ids = animeIds.distinct()
        return ids.flatMap { id -> categories.await(id).map { it.id } }
            .groupingBy { it }.eachCount()
    }

    suspend fun selectSet(syncKey: String) {
        categorySets.selectActiveSet(syncKey)
    }

    suspend fun createInSet(name: String, setId: Long) {
        val result = createCategory.await(name, setId)
        if (result is CreateCategoryWithName.Result.InternalError) throw result.error
    }

    suspend fun save(anime: List<Anime>, changes: Map<Long, Boolean>, addToLibrary: Boolean) {
        setAnimeCategories.awaitBatch(anime.map { it.id }.distinct(), changes, addToLibrary)
    }

    suspend fun invokeSafely(action: suspend () -> Unit): String? = try {
        action()
        null
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        when (error.message) {
            "Category no longer exists. Refresh the picker." ->
                "Kategori sudah dihapus. Pilihan tertunda tidak disimpan; periksa kembali kategori."
            "Unknown target Set" -> "Set tujuan sudah tidak tersedia."
            "Category name is required" -> "Nama kategori harus diisi."
            else -> error.message ?: "Operasi gagal. Coba kembali."
        }
    }
}
