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
    fun missingPersistedSelectionFallsBackAndRepairsPreference() = runBlocking {
        preferences.activeCategorySetKey.set("missing:set")

        repository.observeVisibleCategoriesInActiveSet().first()

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
}
