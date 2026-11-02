package tachiyomi.domain.category.interactor

import dev.zacsweers.metro.Inject
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.repository.AnimeRepository

@Inject
class SetAnimeCategories(
    private val animeRepository: AnimeRepository,
) {

    // Existing replacement API keeps its historical error contract.
    // The picker uses awaitBatch, which propagates failures to the UI.
    suspend fun await(animeId: Long, categoryIds: List<Long>) {
        try {
            animeRepository.setAnimeCategories(animeId, categoryIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitBatch(
        animeIds: List<Long>,
        pendingChanges: Map<Long, Boolean>,
        addToLibrary: Boolean = false,
    ) {
        animeRepository.applyCategoryChanges(animeIds, pendingChanges, addToLibrary)
    }
}
