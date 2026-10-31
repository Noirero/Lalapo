package tachiyomi.domain.category.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategorySet

interface CategorySetRepository {
    fun observeSets(): Flow<List<CategorySet>>
    fun observeVisibleCategoriesInActiveSet(): Flow<List<Category>>
    suspend fun selectActiveSet(syncKey: String)
    suspend fun getActiveSet(): CategorySet
    suspend fun getSets(): List<CategorySet>
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)
    suspend fun reorder(ids: List<Long>)
    suspend fun moveCategories(categoryIds: List<Long>, targetSetId: Long)
    suspend fun delete(id: Long, destinationSetId: Long? = null, deleteCategories: Boolean = false)
}
