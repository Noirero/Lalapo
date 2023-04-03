// AM (CU) -->
package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.model.CustomAnimeInfo
import eu.kanade.domain.entries.anime.repository.CustomAnimeRepository

class SetCustomAnimeInfo(
    private val customAnimeRepository: CustomAnimeRepository,
) {

    fun set(animeInfo: CustomAnimeInfo) = customAnimeRepository.set(animeInfo)
}
// <-- AM (CU)
