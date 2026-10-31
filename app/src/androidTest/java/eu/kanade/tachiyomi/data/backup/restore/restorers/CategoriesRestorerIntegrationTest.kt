package eu.kanade.tachiyomi.data.backup.restore.restorers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.async.coroutines.awaitAsList
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupCategorySet
import kotlinx.coroutines.runBlocking
import mihon.app.di.AppBindings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.data.category.CategoryRepositoryImpl
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.library.service.LibraryPreferences

@RunWith(AndroidJUnit4::class)
class CategoriesRestorerIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var driver: app.cash.sqldelight.db.SqlDriver
    private lateinit var database: tachiyomi.data.Database
    private lateinit var restorer: CategoriesRestorer
    private val preferencesName = "categories_restore_integration_test"

    @Before
    fun setUp() {
        context.deleteDatabase("tachiyomi.animedb")
        context.getSharedPreferences(preferencesName, 0).edit().clear().commit()
        driver = AppBindings.providesSqlDriver(context)
        database = AppBindings.providesDatabase(driver)
        restorer = CategoriesRestorer(
            database,
            GetCategories(CategoryRepositoryImpl(database)),
            LibraryPreferences(AndroidPreferenceStore(context.getSharedPreferences(preferencesName, 0))),
        )
    }

    @After
    fun tearDown() {
        driver.close()
        context.deleteDatabase("tachiyomi.animedb")
        context.getSharedPreferences(preferencesName, 0).edit().clear().commit()
    }

    @Test
    fun categoryIdsAreRemappedAndRepeatedRestoreIsIdempotent() = runBlocking {
        database.categoriesQueries.insert("Existing", 0, 0)
        val existingId = database.categoriesQueries.getCategories().awaitAsList()
            .first { it.name == "Existing" }.id
        val categories = listOf(
            BackupCategory("Existing", 0, 900),
            BackupCategory("Restored", 1, 901),
        )
        val sets = listOf(BackupCategorySet("backup:set:a", "Imported", 0, listOf(900, 901)))
        restorer(categories, sets)
        val restoredId = database.categoriesQueries.getCategories().awaitAsList()
            .first { it.name == "Restored" }.id
        assertTrue(restoredId != 901L)
        assertEquals(
            existingId,
            database.categoriesQueries.getCategories().awaitAsList()
                .first { it.name == "Existing" }.id,
        )
        val firstSets = database.category_setsQueries.getSets().awaitAsList()
        val firstAssignments = database.category_setsQueries.getSetAssignments().awaitAsList()
        val imported = firstSets.single { it.sync_key == "backup:set:a" }
        assertEquals(
            setOf(existingId, restoredId),
            firstAssignments
                .filter { it.set_id == imported._id }.map { it.category_id }.toSet(),
        )
        restorer(categories, sets)
        assertEquals(
            firstSets.map { it.sync_key to it.sort },
            database.category_setsQueries.getSets().awaitAsList().map { it.sync_key to it.sort },
        )
        assertEquals(
            firstAssignments.map { it.category_id to it.set_id },
            database.category_setsQueries.getSetAssignments().awaitAsList().map { it.category_id to it.set_id },
        )
    }

    @Test
    fun duplicateSyncKeyInPayloadReusesOnePersistedSet() = runBlocking {
        val categories = listOf(
            BackupCategory("First", 0, 501),
            BackupCategory("Second", 1, 502),
        )
        val sets = listOf(
            BackupCategorySet("duplicate:key", "Shared", 0, listOf(501)),
            BackupCategorySet("duplicate:key", "Shared", 1, listOf(502)),
        )
        restorer(categories, sets)
        val persistedSets = database.category_setsQueries.getSets().awaitAsList()
        assertEquals(1, persistedSets.count { it.sync_key == "duplicate:key" })
        val targetId = persistedSets.single { it.sync_key == "duplicate:key" }._id
        val assignments = database.category_setsQueries.getSetAssignments().awaitAsList()
        assertEquals(2, assignments.count { it.set_id == targetId })
        assertEquals(2, assignments.map { it.category_id }.distinct().size)
    }

    @Test
    fun emptyCategoriesStillRestoreEmptySets() = runBlocking {
        restorer(
            emptyList(),
            listOf(BackupCategorySet("backup:set:empty", "Empty Set", 0, emptyList())),
        )

        val restoredSet = database.category_setsQueries.getSets().awaitAsList()
            .single { it.sync_key == "backup:set:empty" }
        assertEquals("Empty Set", restoredSet.name)
        assertTrue(
            database.category_setsQueries.getSetAssignments().awaitAsList()
                .none { it.set_id == restoredSet._id },
        )
    }

    @Test
    fun existingSetMetadataAndOrderAreRestored() = runBlocking {
        restorer(
            emptyList(),
            listOf(
                BackupCategorySet("backup:set:first", "First", 0, emptyList()),
                BackupCategorySet("lalapo:category-set:default", "Renamed Default", 1, emptyList()),
            ),
        )

        val sets = database.category_setsQueries.getSets().awaitAsList()
        assertEquals(
            listOf("backup:set:first", "lalapo:category-set:default"),
            sets.map { it.sync_key },
        )
        assertEquals(listOf(0L, 1L), sets.map { it.sort })
        assertEquals(
            "Renamed Default",
            sets.single { it.sync_key == "lalapo:category-set:default" }.name,
        )
    }

    @Test
    fun oldBackupWithoutSetsPreservesDefaultAssignments() = runBlocking {
        restorer(listOf(BackupCategory("Legacy", 0, 777)), emptyList())
        val category = database.categoriesQueries.getCategories().awaitAsList()
            .single { it.name == "Legacy" }
        val assignment = database.category_setsQueries.getSetAssignments().awaitAsList()
            .single { it.category_id == category.id }
        assertEquals(
            "lalapo:category-set:default",
            database.category_setsQueries.getSets().awaitAsList()
                .single { it._id == assignment.set_id }.sync_key,
        )
    }
}
