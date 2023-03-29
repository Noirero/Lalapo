// AM (CU) -->
package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.entries.anime.repository.AnimeRepository

class GetAllAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): List<Anime> {
        return animeRepository.getAll()
    }
}
// <-- AM (CU)
