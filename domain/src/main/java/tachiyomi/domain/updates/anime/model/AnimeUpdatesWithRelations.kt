package tachiyomi.domain.updates.anime.model

import tachiyomi.domain.entries.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.entries.anime.model.AnimeCover
import uy.kohesive.injekt.injectLazy

data class AnimeUpdatesWithRelations(
    val animeId: Long,
    // AM (CUSTOM_INFORMATION) -->
    val ogAnimeTitle: String,
    // <-- AM (CUSTOM_INFORMATION)
    val episodeId: Long,
    val episodeName: String,
    val scanlator: String?,
    val seen: Boolean,
    val bookmark: Boolean,
    // AM (FILLERMARK) -->
    val fillermark: Boolean,
    // <-- AM (FILLERMARK)
    val lastSecondSeen: Long,
    val totalSeconds: Long,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: AnimeCover,
) {
    // AM (CUSTOM_INFORMATION) -->
    val animeTitle: String = getCustomAnimeInfo.get(animeId)?.title ?: ogAnimeTitle

    companion object {
        private val getCustomAnimeInfo: GetCustomAnimeInfo by injectLazy()
    }
    // <-- AM (CUSTOM_INFORMATION)
}
