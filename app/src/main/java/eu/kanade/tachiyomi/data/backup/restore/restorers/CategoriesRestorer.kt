package eu.kanade.tachiyomi.data.backup.restore.restorers

import app.cash.sqldelight.async.coroutines.awaitAsList
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupCategorySet
import tachiyomi.data.Database
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.library.service.LibraryPreferences

@Inject
class CategoriesRestorer(
    private val database: Database,
    private val getCategories: GetCategories,
    private val libraryPreferences: LibraryPreferences,
) {

    private suspend fun restoreSets(
        backupCategories: List<BackupCategory>,
        restoredCategories: List<tachiyomi.domain.category.model.Category>,
        backupSets: List<BackupCategorySet>,
    ) {
        // Restore maps by backup category identity, never by raw database ID.
        val categoryIdMap = backupCategories.zip(restoredCategories).associate { (old, current) ->
            old.id to current.id
        }
        database.transaction {
            val existing = database.category_setsQueries.getSets().awaitAsList()
            backupSets.sortedBy { it.order }.forEach { backupSet ->
                val matched = existing.firstOrNull { it.sync_key == backupSet.syncKey }
                val targetId = if (matched != null) {
                    matched._id
                } else {
                    val uniqueKey = backupSet.syncKey.takeIf { key ->
                        key.isNotBlank() && existing.none { it.sync_key == key }
                    } ?: java.util.UUID.randomUUID().toString()
                    database.category_setsQueries.insertSet(
                        uniqueKey,
                        backupSet.name.trim().ifEmpty { "Restored Set" },
                        (existing.maxOfOrNull { it.sort } ?: 0L) + backupSet.order + 1L,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                    )
                    database.category_setsQueries.getSetBySyncKey(uniqueKey).awaitAsList().first()._id
                }
                backupSet.categoryIds.mapNotNull(categoryIdMap::get).forEach { categoryId ->
                    database.category_setsQueries.assignCategory(categoryId, targetId)
                }
            }
        }
    }

    suspend operator fun invoke(backupCategories: List<BackupCategory>, backupSets: List<BackupCategorySet> = emptyList()) {
        if (backupCategories.isNotEmpty()) {
            val dbCategories = getCategories.await()
            val dbCategoriesByName = dbCategories.associateBy { it.name }
            var nextOrder = dbCategories.maxOfOrNull { it.order }?.plus(1) ?: 0

            val categories = database.transactionWithResult {
                backupCategories
                    .sortedBy { it.order }
                    .map {
                        val dbCategory = dbCategoriesByName[it.name]
                        if (dbCategory != null) return@map dbCategory
                        val order = nextOrder++
                        database.categoriesQueries
                            .insert(it.name, order, it.flags)
                            .let { id -> it.toCategory(id).copy(order = order) }
                    }
            }

            if (backupSets.isNotEmpty()) {
                restoreSets(backupCategories, categories, backupSets)
            }

            libraryPreferences.categorizedDisplaySettings.set(
                (dbCategories + categories)
                    .distinctBy { it.flags }
                    .size > 1,
            )
        }
    }
}
