package tachiyomi.data.category

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.domain.category.model.CategorySet
import tachiyomi.domain.category.repository.CategorySetRepository

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CategorySetRepositoryImpl(
    private val database: Database,
) : CategorySetRepository {

    override fun observeSets(): Flow<List<CategorySet>> =
        database.category_setsQueries.getSets().subscribeToList().map { rows ->
            rows.map { CategorySet(it._id, it.sync_key, it.name, it.sort, it.created_at, it.updated_at) }
        }

    override suspend fun getSets(): List<CategorySet> =
        database.category_setsQueries.getSets().awaitAsList().map {
            CategorySet(it._id, it.sync_key, it.name, it.sort, it.created_at, it.updated_at)
        }

    override suspend fun create(name: String): Long {
        val normalized = name.trim()
        require(normalized.isNotEmpty())
        var createdId = 0L
        database.transaction {
            val existing = getSets()
            require(existing.none { it.name.trim().equals(normalized, ignoreCase = true) }) { "Duplicate Set name" }
            val now = System.currentTimeMillis()
            database.category_setsQueries.insertSet(
                syncKey = UUID.randomUUID().toString(),
                name = normalized,
                sort = (existing.maxOfOrNull { it.order } ?: -1L) + 1L,
                createdAt = now,
                updatedAt = now,
            )
            createdId = database.category_setsQueries.getSets().awaitAsList()
                .first { it.name == normalized }. _id
        }
        return createdId
    }

    override suspend fun rename(id: Long, name: String) {
        val normalized = name.trim()
        require(normalized.isNotEmpty())
        database.transaction {
            val sets = getSets()
            require(sets.any { it.id == id }) { "Unknown Set" }
            require(sets.none { it.id != id && it.name.trim().equals(normalized, ignoreCase = true) }) { "Duplicate Set name" }
            database.category_setsQueries.renameSet(normalized, System.currentTimeMillis(), id)
        }
    }

    override suspend fun reorder(ids: List<Long>) {
        database.transaction {
            val existing = getSets()
            require(ids.size == existing.size && ids.toSet() == existing.map { it.id }.toSet())
            ids.forEachIndexed { index, id ->
                database.category_setsQueries.reorderSet(index.toLong(), System.currentTimeMillis(), id)
            }
        }
    }

    override suspend fun moveCategories(categoryIds: List<Long>, targetSetId: Long) {
        database.transaction {
            require(getSets().any { it.id == targetSetId }) { "Unknown target Set" }
            categoryIds.distinct().forEach { categoryId ->
                require(categoryId > 0L)
                require(database.categoriesQueries.getCategory(categoryId).awaitAsList().isNotEmpty()) { "Unknown category" }
                database.category_setsQueries.assignCategory(categoryId, targetSetId)
            }
        }
    }

    override suspend fun delete(id: Long, destinationSetId: Long?, deleteCategories: Boolean) {
        database.transaction {
            val sets = getSets()
            require(sets.size > 1 && sets.any { it.id == id }) { "Cannot delete last or unknown Set" }
            val categories = database.category_setsQueries.getCategoriesInSet(id).awaitAsList()
            if (categories.isNotEmpty()) {
                if (deleteCategories) {
                    categories.forEach { database.categoriesQueries.delete(it._id) }
                } else {
                    require(destinationSetId != null && destinationSetId != id && sets.any { it.id == destinationSetId })
                    database.category_setsQueries.moveSetCategories(destinationSetId, id)
                }
            }
            database.category_setsQueries.deleteSet(id)
        }
    }
}
