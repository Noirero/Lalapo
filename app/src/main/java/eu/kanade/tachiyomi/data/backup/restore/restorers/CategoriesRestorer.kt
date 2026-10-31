package eu.kanade.tachiyomi.data.backup.restore.restorers

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
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
        val categoryIdMap = backupCategories.sortedBy { it.order }.zip(restoredCategories).associate { (old, current) ->
            old.id to current.id
        }
        database.transaction {
            backupSets.sortedBy { it.order }.forEach { backupSet ->
                // Re-query inside the transaction so duplicate keys in a single backup
                // and repeat restores resolve to the same Set, not duplicate inserts.
                val existing = database.category_setsQueries.getSets().awaitAsList()
                val matched = existing.firstOrNull { it.sync_key == backupSet.syncKey }
                val restoredName = backupSet.name.trim().ifEmpty { "Restored Set" }
                val now = System.currentTimeMillis()
                val targetId = if (matched != null) {
                    database.category_setsQueries.renameSet(restoredName, now, matched._id)
                    database.category_setsQueries.reorderSet(backupSet.order, now, matched._id)
                    matched._id
                } else {
                    val uniqueKey = backupSet.syncKey.takeIf { key ->
                        key.isNotBlank() && existing.none { it.sync_key == key }
                    } ?: java.util.UUID.randomUUID().toString()
                    database.category_setsQueries.insertSet(
                        uniqueKey,
                        restoredName,
                        backupSet.order,
                        now,
                        now,
                    )
                    database.category_setsQueries.getSetBySyncKey(uniqueKey).awaitAsList().first()._id
                }
                backupSet.categoryIds.mapNotNull(categoryIdMap::get).forEach { categoryId ->
                    database.category_setsQueries.assignCategory(categoryId, targetId)
                }
            }
        }
    }

    suspend operator fun invoke(
        backupCategories: List<BackupCategory>,
        backupSets: List<BackupCategorySet> = emptyList(),
    ) {
        if (backupCategories.isEmpty()) {
            if (backupSets.isNotEmpty()) {
                restoreSets(emptyList(), emptyList(), backupSets)
            }
            return
        }

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
                        database.categoriesQueries.insert(it.name, order, it.flags)
                        val persistedId = database.categoriesQueries.getCategoryByNameAndOrder(it.name, order)
                            .awaitAsOne().id
                        it.toCategory(persistedId).copy(order = order)
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
