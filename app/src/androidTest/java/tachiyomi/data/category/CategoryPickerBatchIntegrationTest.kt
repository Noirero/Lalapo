package tachiyomi.data.category

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import mihon.app.di.AppBindings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.data.anime.AnimeRepositoryImpl
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences

@RunWith(AndroidJUnit4::class)
class CategoryPickerBatchIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var driver: app.cash.sqldelight.db.SqlDriver
    private lateinit var database: tachiyomi.data.Database
    private lateinit var sets: CategorySetRepositoryImpl
    private lateinit var animeRepository: AnimeRepositoryImpl
    private val preferencesName = "picker_batch_integration_test"

    @Before
    fun setUp() {
        context.deleteDatabase("tachiyomi.animedb")
        context.getSharedPreferences(preferencesName, 0).edit().clear().commit()
        driver = AppBindings.providesSqlDriver(context)
        database = AppBindings.providesDatabase(driver)
        sets = CategorySetRepositoryImpl(
            database,
            CategoryRepositoryImpl(database),
            LibraryPreferences(AndroidPreferenceStore(context.getSharedPreferences(preferencesName, 0))),
        )
        animeRepository = AnimeRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        driver.close()
        context.deleteDatabase("tachiyomi.animedb")
        context.getSharedPreferences(preferencesName, 0).edit().clear().commit()
    }

    private suspend fun category(name: String, setId: Long): Long {
        val order = database.categoriesQueries.getCategories().awaitAsList().size.toLong()
        sets.createCategoryInSet(Category(0, name, order, 0, false), setId)
        return database.categoriesQueries.getCategories().awaitAsList().single { it.name == name }.id
    }

    private suspend fun anime(url: String, isFavorite: Boolean = true): Long {
        return database.animesQueries.insertReturningId(
            source = 1L,
            url = url,
            artist = null,
            author = null,
            description = null,
            genre = null,
            title = url,
            status = 0L,
            thumbnailUrl = null,
            favorite = isFavorite,
            lastUpdate = 0L,
            nextUpdate = 0L,
            calculateInterval = 0L,
            initialized = true,
            viewerFlags = 0L,
            episodeFlags = 0L,
            coverLastModified = 0L,
            dateAdded = 0L,
            updateStrategy = AnimeUpdateStrategy.ALWAYS_UPDATE,
            version = 0L,
            notes = "",
            fetchType = FetchType.Episodes,
            parentId = null,
            seasonFlags = 0L,
            seasonNumber = 0.0,
            seasonSourceOrder = 0L,
            backgroundUrl = null,
            backgroundLastModified = 0L,
            memo = JsonObject(emptyMap()),
        ).awaitAsOne()
    }

    private suspend fun memberships(animeId: Long): Set<Long> {
        return database.categoriesQueries.getCategoriesByAnimeId(animeId).awaitAsList().map { it.id }.toSet()
    }

    @Test
    fun bulkChangePreservesHiddenAndOtherSetMemberships() = runBlocking {
        val default = sets.getSets().single().id
        val second = sets.create("Second")
        val hidden = category("Hidden", default)
        val other = category("Other Set", second)
        val shared = category("Shared", default)
        database.categoriesQueries.updateHidden(hidden = 1L, categoryId = hidden)
        val a = anime("test://bulk-a")
        val b = anime("test://bulk-b")
        database.animes_categoriesQueries.insert(a, hidden)
        database.animes_categoriesQueries.insert(a, other)
        database.animes_categoriesQueries.insert(a, shared)
        database.animes_categoriesQueries.insert(b, hidden)

        animeRepository.applyCategoryChanges(listOf(a, b), mapOf(shared to true, other to false))

        assertEquals(setOf(hidden, shared), memberships(a))
        assertEquals(setOf(hidden, shared), memberships(b))
        assertEquals(true, database.categoriesQueries.getCategory(hidden).awaitAsList().single().hidden == 1L)
    }

    @Test
    fun invalidOrDeletedCategoryAbortsWholeBatch() = runBlocking {
        val default = sets.getSets().single().id
        val categoryId = category("Persist", default)
        val a = anime("test://rollback-a")
        val b = anime("test://rollback-b")
        database.animes_categoriesQueries.insert(a, categoryId)
        assertTrue(
            runCatching {
                animeRepository.applyCategoryChanges(
                    listOf(a, b),
                    mapOf(categoryId to false, -55L to true),
                )
            }.isFailure,
        )
        assertEquals(setOf(categoryId), memberships(a))
        assertEquals(emptySet<Long>(), memberships(b))
        assertTrue(
            runCatching {
                animeRepository.applyCategoryChanges(listOf(a, b), mapOf(999999L to true))
            }.isFailure,
        )
        assertEquals(setOf(categoryId), memberships(a))
    }

    @Test
    fun addToLibraryAndMembershipAreCommittedTogether() = runBlocking {
        val setId = sets.create("Later")
        val target = category("Later category", setId)
        val a = anime("test://new-library", isFavorite = false)
        assertFalse(animeRepository.getAnimeById(a).favorite)

        animeRepository.applyCategoryChanges(listOf(a), mapOf(target to true), addToLibrary = true)

        assertEquals(setOf(target), memberships(a))
        assertTrue(animeRepository.getAnimeById(a).favorite)
    }

    @Test
    fun noPendingChangesDoesNotModifyMemberships() = runBlocking {
        val categoryId = category("Untouched", sets.getSets().single().id)
        val a = anime("test://cancel")
        database.animes_categoriesQueries.insert(a, categoryId)
        animeRepository.applyCategoryChanges(listOf(a), emptyMap())

        assertEquals(setOf(categoryId), memberships(a))
    }

    @Test
    fun movedCategoryRetainsIdentityAndPendingSelection() = runBlocking {
        val source = sets.getSets().single().id
        val target = sets.create("Target")
        val categoryId = category("Moving", source)
        val animeId = anime("test://move")
        sets.moveCategories(listOf(categoryId), target)

        animeRepository.applyCategoryChanges(listOf(animeId), mapOf(categoryId to true))

        assertEquals(setOf(categoryId), memberships(animeId))
        assertEquals(target, database.category_setsQueries.getCategorySet(categoryId).awaitAsOne())
    }
}
