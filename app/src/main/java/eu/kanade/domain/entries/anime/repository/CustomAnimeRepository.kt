// AM (CU) -->
package eu.kanade.domain.entries.anime.repository

import eu.kanade.domain.entries.anime.model.CustomAnimeInfo

interface CustomAnimeRepository {

    fun get(animeId: Long): CustomAnimeInfo?

    fun set(animeInfo: CustomAnimeInfo)
}
// <-- AM (CU)
