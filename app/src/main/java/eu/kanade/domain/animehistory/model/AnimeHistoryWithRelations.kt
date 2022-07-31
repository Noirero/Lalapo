package eu.kanade.domain.animehistory.model

import eu.kanade.domain.anime.model.AnimeCover
import eu.kanade.tachiyomi.data.animelib.CustomAnimeManager
import uy.kohesive.injekt.injectLazy
import java.util.Date

data class AnimeHistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    // AM -->
    val ogTitle: String,
    // AM <--
    val episodeNumber: Float,
    val seenAt: Date?,
    val coverData: AnimeCover,
) {
    // AM -->
    val title: String = customAnimeManager.getAnime(animeId)?.title ?: ogTitle

    companion object {
        private val customAnimeManager: CustomAnimeManager by injectLazy()
    }
    // AM <--
}
