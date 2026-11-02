package tachiyomi.domain.category.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.anime.repository.AnimeRepository

@Inject
class SetAnimeCategories(
    private val animeRepository: AnimeRepository,
) {

    // Keep the legacy replacement entry point, but never conceal a failed write.
    suspend fun await(animeId: Long, categoryIds: List<Long>) {
        animeRepository.setAnimeCategories(animeId, categoryIds)
    }

    suspend fun awaitBatch(
        animeIds: List<Long>,
        pendingChanges: Map<Long, Boolean>,
        addToLibrary: Boolean = false,
    ) {
        animeRepository.applyCategoryChanges(animeIds, pendingChanges, addToLibrary)
    }
}
