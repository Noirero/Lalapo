package tachiyomi.domain.updates.anime.model

import eu.kanade.tachiyomi.data.library.anime.CustomAnimeManager
import tachiyomi.domain.entries.anime.model.AnimeCover
import uy.kohesive.injekt.injectLazy

data class AnimeUpdatesWithRelations(
    val animeId: Long,
    // AM (CU) -->
    val ogAnimeTitle: String,
    // <-- AM (CU)
    val episodeId: Long,
    val episodeName: String,
    val scanlator: String?,
    val seen: Boolean,
    val bookmark: Boolean,
    val fillermark: Boolean,
    val lastSecondSeen: Long,
    val totalSeconds: Long,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: AnimeCover,
) {
    // AM (CU) -->
    val animeTitle: String = customAnimeManager.getAnime(animeId)?.title ?: ogAnimeTitle

    companion object {
        private val customAnimeManager: CustomAnimeManager by injectLazy()
    }
    // <-- AM (CU)
}
