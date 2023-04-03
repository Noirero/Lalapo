// AM (CU) -->
package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.repository.CustomAnimeRepository

class GetCustomAnimeInfo(
    private val customAnimeRepository: CustomAnimeRepository,
) {

    fun get(animeId: Long) = customAnimeRepository.get(animeId)
}
// <-- AM (CU)
