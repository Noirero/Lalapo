package eu.kanade.data.updates.anime

import eu.kanade.domain.entries.anime.model.AnimeCover
import eu.kanade.domain.updates.anime.model.AnimeUpdatesWithRelations

val animeUpdateWithRelationMapper: (Long, String, Long, String, String?, Boolean, Boolean, /* AM (FM) --> */ Boolean, /* <-- AM (FM) */ Long, Boolean, String?, Long, Long, Long) -> AnimeUpdatesWithRelations = {
        animeId, animeTitle, episodeId, episodeName, scanlator, seen, bookmark, /* AM (FM) --> */ fillermark, /* <-- AM (FM) */ sourceId, favorite, thumbnailUrl, coverLastModified, _, dateFetch ->
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
