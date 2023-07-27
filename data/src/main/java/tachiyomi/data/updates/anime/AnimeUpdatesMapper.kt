package tachiyomi.data.updates.anime

import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.updates.anime.model.AnimeUpdatesWithRelations

// AM (FM)>
val animeUpdateWithRelationMapper: (Long, String, Long, String, String?, Boolean, Boolean, Boolean, Long, Long, Long, Boolean, String?, Long, Long, Long) -> AnimeUpdatesWithRelations = {
        animeId, animeTitle, episodeId, episodeName, scanlator, seen, bookmark, fillermark, lastSecondSeen, totalSeconds, sourceId, favorite, thumbnailUrl, coverLastModified, _, dateFetch ->
    AnimeUpdatesWithRelations(
        animeId = animeId,
        // AM (CU) -->
        ogAnimeTitle = animeTitle,
        // <-- AM (CU)
        episodeId = episodeId,
        episodeName = episodeName,
        scanlator = scanlator,
        seen = seen,
        bookmark = bookmark,
        // AM (FM) -->
        fillermark = fillermark,
        // <-- AM (FM)
        lastSecondSeen = lastSecondSeen,
        totalSeconds = totalSeconds,
        sourceId = sourceId,
        dateFetch = dateFetch,
        coverData = AnimeCover(
            animeId = animeId,
            sourceId = sourceId,
            isAnimeFavorite = favorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )
}
