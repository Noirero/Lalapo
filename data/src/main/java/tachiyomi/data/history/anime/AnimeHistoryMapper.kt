package tachiyomi.data.history.anime

import java.util.Date
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.history.anime.model.AnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations

object AnimeHistoryMapper {
    fun mapAnimeHistory(
        id: Long,
        episodeId: Long,
        seenAt: Date?,
    ): AnimeHistory = AnimeHistory(
        id = id,
        episodeId = episodeId,
        seenAt = seenAt,
    )

    fun mapAnimeHistoryWithRelations(
        historyId: Long,
        animeId: Long,
        episodeId: Long,
        title: String,
        thumbnailUrl: String?,
        sourceId: Long,
        isFavorite: Boolean,
        coverLastModified: Long,
        episodeNumber: Double,
        seenAt: Date?,
    ): AnimeHistoryWithRelations = AnimeHistoryWithRelations(
        id = historyId,
        episodeId = episodeId,
        animeId = animeId,
        // AM (CU) -->
        ogTitle = title,
        // <-- AM (CU)
        episodeNumber = episodeNumber,
        seenAt = seenAt,
        coverData = AnimeCover(
            animeId = animeId,
            sourceId = sourceId,
            isAnimeFavorite = isFavorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )
}
