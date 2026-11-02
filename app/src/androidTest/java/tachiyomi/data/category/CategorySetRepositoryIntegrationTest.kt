package tachiyomi.data.category

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mihon.app.di.AppBindings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences

@RunWith(AndroidJUnit4::class)
class CategorySetRepositoryIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var driver: app.cash.sqldelight.db.SqlDriver
    private lateinit var database: tachiyomi.data.Database
    private lateinit var preferences: LibraryPreferences
    private lateinit var repository: CategorySetRepositoryImpl
    private val preferencesName = "category_set_repository_integration_test"

    @Before
    fun setUp() {
        context.deleteDatabase("tachiyomi.animedb")
        context.getSharedPreferences(preferencesName, 0).edit().clear().commit()
        driver = AppBindings.providesSqlDriver(context)
        database = AppBindings.providesDatabase(driver)
        preferences = LibraryPreferences(
            AndroidPreferenceStore(context.getSharedPreferences(preferencesName, 0)),
        )
        repository = CategorySetRepositoryImpl(
            database = database,
            categoryRepository = CategoryRepositoryImpl(database),
            libraryPreferences = preferences,
        )
    }

    @After
    fun tearDown() {
        driver.close()
        context.deleteDatabase("tachiyomi.animedb")
        context.getSharedPreferences(preferencesName, 0).edit().clear().commit()
    }

    @Test
    fun observingSetsRepairsMissingPersistedSelection() = runBlocking {
        preferences.activeCategorySetKey.set("missing:set")

        repository.observeSets().first()

        assertEquals(
            "lalapo:category-set:default",
            preferences.activeCategorySetKey.get(),
        )
    }

    @Test
    fun selectingSetUpdatesObservedCategoriesWithoutRestart() = runBlocking {
        val defaultSet = repository.getSets().single()
        database.categoriesQueries.insert("Default category", 0, 0)
        val defaultCategoryId = database.categoriesQueries.getCategories().awaitAsList()
            .single { it.name == "Default category" }.id
        database.category_setsQueries.assignCategory(defaultCategoryId, defaultSet.id)

        val secondSetId = repository.create("Second")
        val secondSet = repository.getSets().single { it.id == secondSetId }
        database.categoriesQueries.insert("Second category", 1, 0)
        val secondCategoryId = database.categoriesQueries.getCategories().awaitAsList()
            .single { it.name == "Second category" }.id
        repository.moveCategories(listOf(secondCategoryId), secondSet.id)

        val changed = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) {
                repository.observeVisibleCategoriesInActiveSet().first { categories ->
                    categories.any { it.id == secondCategoryId } &&
                        categories.none { it.id == defaultCategoryId }
                }
            }
        }

        repository.selectActiveSet(secondSet.syncKey)
        val categories = changed.await()

        assertTrue(categories.any { it.id == secondCategoryId })
        assertFalse(categories.any { it.id == defaultCategoryId })
        assertEquals(secondSet.syncKey, preferences.activeCategorySetKey.get())
    }
    private fun newCategory(name: String, order: Long = 0L) = Category(
        id = 0L,
        name = name,
        order = order,
        flags = 0L,
        hidden = false,
    )

    private suspend fun categoryId(name: String) = database.categoriesQueries.getCategories()
        .awaitAsList().single { it.name == name }.id

    private suspend fun assignment(categoryId: Long) = database.category_setsQueries.getSetAssignments()
        .awaitAsList().single { it.category_id == categoryId }.set_id

    @Test
    fun scopedCreateAssignsNonDefaultSetAtomically() = runBlocking {
        val target = repository.create("Second")
        val create = CreateCategoryWithName(CategoryRepositoryImpl(database), repository, preferences)
        assertEquals(CreateCategoryWithName.Result.Success, create.await("Target only", target))
        val category = categoryId("Target only")

        assertEquals(target, assignment(category))
        assertEquals(setOf(category), repository.observeCategoriesBySet().first()[target]!!.map { it.id }.toSet())
        assertTrue(repository.observeCategoriesBySet().first()[repository.getSets().first().id].isNullOrEmpty())
    }

    @Test
    fun genericCreateStillUsesDatabaseDefaultAssignment() = runBlocking {
        val expectedDefault = repository.getSets().single().id
        repository.create("Second")
        val create = CreateCategoryWithName(CategoryRepositoryImpl(database), repository, preferences)
        assertEquals(CreateCategoryWithName.Result.Success, create.await("Generic"))

        assertEquals(expectedDefault, assignment(categoryId("Generic")))
    }

    @Test
    fun invalidScopedCreationDoesNotInsertOrLeaveAnAssignment() = runBlocking {
        assertTrue(runCatching { repository.createCategoryInSet(newCategory("Invalid"), -123L) }.isFailure)
        assertTrue(database.categoriesQueries.getCategories().awaitAsList().none { it.name == "Invalid" })
        assertTrue(database.category_setsQueries.getSetAssignments().awaitAsList().isEmpty())
    }

    @Test
    fun movingCategoryPreservesIdentityFlagsHiddenStateAndAnimeMembership() = runBlocking {
        val destination = repository.create("Destination")
        val source = repository.getSets().first().id
        CategoryRepositoryImpl(database).insert(newCategory("Assigned", 3))
        val id = categoryId("Assigned")
        database.categoriesQueries.updateHidden(1L, id)

        // Populate an actual anime/category FK membership and confirm it survives reassignment.
        val sqlite = context.openOrCreateDatabase("tachiyomi.animedb", 0, null)
        try {
            sqlite.execSQL(
                """INSERT INTO animes(source, url, title, status, favorite, initialized, viewer,
                   episode_flags, cover_last_modified, date_added, season_flags, season_number,
                   season_source_order, background_last_modified)
                   VALUES (1, 'test://set-move', 'Test', 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0)""",
            )
            val cursor = sqlite.rawQuery("SELECT _id FROM animes WHERE url = 'test://set-move'", null)
            val animeId = cursor.use {
                it.moveToFirst()
                it.getLong(0)
            }
            sqlite.execSQL(
                "INSERT INTO animes_categories(anime_id, category_id) VALUES (?, ?)",
                arrayOf(animeId, id),
            )
            assertEquals(source, assignment(id))
            repository.moveCategories(listOf(id), destination)
            assertEquals(destination, assignment(id))
            val category = CategoryRepositoryImpl(database).get(id)!!
            assertEquals("Assigned", category.name)
            assertEquals(3L, category.order)
            assertTrue(category.hidden)
            val membership = sqlite.rawQuery(
                "SELECT category_id FROM animes_categories WHERE anime_id = ?",
                arrayOf(animeId.toString()),
            )
            membership.use {
                assertTrue(it.moveToFirst())
                assertEquals(id, it.getLong(0))
            }
        } finally {
            sqlite.close()
        }
    }

    @Test
    fun nonemptyDeleteMovesCategoriesToChosenDestinationAndRepairsActiveSet() = runBlocking {
        val source = repository.create("Source")
        val target = repository.create("Target")
        val active = repository.getSets().single { it.id == source }
        repository.createCategoryInSet(newCategory("Saved"), source)
        val categoryId = categoryId("Saved")
        repository.selectActiveSet(active.syncKey)

        repository.delete(source, target)

        assertEquals(target, assignment(categoryId))
        assertEquals("Saved", CategoryRepositoryImpl(database).get(categoryId)?.name)
        assertTrue(repository.getSets().none { it.id == source })
        assertEquals(repository.getSets().first().syncKey, preferences.activeCategorySetKey.get())
    }

    @Test
    fun missingSameOrUnknownDestinationRejectsDeleteWithoutDataLoss() = runBlocking {
        val source = repository.create("Source")
        repository.createCategoryInSet(newCategory("Safe"), source)
        val id = categoryId("Safe")
        for (invalidTarget in listOf(null, source, -42L)) {
            assertTrue(runCatching { repository.delete(source, invalidTarget) }.isFailure)
            assertEquals(source, assignment(id))
            assertTrue(repository.getSets().any { it.id == source })
        }
    }

    @Test
    fun cannotDeleteFinalSetAndCanDeleteEmptySetWithoutDestination() = runBlocking {
        val original = repository.getSets().single().id
        assertTrue(runCatching { repository.delete(original, null) }.isFailure)
        val empty = repository.create("Empty")
        repository.delete(empty, null)
        assertEquals(original, repository.getSets().single().id)
    }

    @Test
    fun namesRejectBlankAndDuplicatesCaseInsensitively() = runBlocking {
        val first = repository.create("Work")
        assertTrue(runCatching { repository.create("   ") }.isFailure)
        assertTrue(runCatching { repository.create("work") }.isFailure)
        assertTrue(runCatching { repository.rename(first, "  ") }.isFailure)
        assertTrue(runCatching { repository.rename(repository.getSets().first().id, "WORK") }.isFailure)
        assertEquals("Work", repository.getSets().single { it.id == first }.name)
    }

    @Test
    fun reorderRetainsSetIdsSyncKeysAndEmptyMembership() = runBlocking {
        val second = repository.create("Second")
        val third = repository.create("Third")
        val before = repository.getSets().associateBy { it.id }
        repository.reorder(listOf(third, second, before.keys.first()))
        val after = repository.getSets()

        assertEquals(listOf(third, second, before.keys.first()), after.map { it.id })
        assertEquals(before.mapValues { it.value.syncKey }, after.associate { it.id to it.syncKey })
        assertTrue(repository.observeCategoriesBySet().first().isEmpty())
    }
}
