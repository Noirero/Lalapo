package tachiyomi.domain.history.anime.model

import tachiyomi.domain.entries.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.entries.anime.model.AnimeCover
import uy.kohesive.injekt.injectLazy
import java.util.Date

data class AnimeHistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    // AM (CUSTOM) -->
    val ogTitle: String,
    // <-- AM (CUSTOM)
    val episodeNumber: Double,
    val seenAt: Date?,
    val coverData: AnimeCover,
) {
    // AM (CUSTOM) -->
    val title: String = customAnimeManager.get(animeId)?.title ?: ogTitle

    companion object {
        private val customAnimeManager: GetCustomAnimeInfo by injectLazy()
    }
    // <-- AM (CUSTOM)
}
