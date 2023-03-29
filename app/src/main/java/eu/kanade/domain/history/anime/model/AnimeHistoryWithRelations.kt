package eu.kanade.domain.history.anime.model

import eu.kanade.domain.entries.anime.interactor.GetCustomAnimeInfo
import eu.kanade.domain.entries.anime.model.AnimeCover
import uy.kohesive.injekt.injectLazy
import java.util.Date

data class AnimeHistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    // AM (CU) -->
    val ogTitle: String,
    // <-- AM (CU)
    val episodeNumber: Float,
    val seenAt: Date?,
    val coverData: AnimeCover,
) {
    // AM (CU) -->
    val title: String = customAnimeManager.get(animeId)?.title ?: ogTitle

    companion object {
        private val customAnimeManager: GetCustomAnimeInfo by injectLazy()
    }
    // <-- AM (CU)
}
