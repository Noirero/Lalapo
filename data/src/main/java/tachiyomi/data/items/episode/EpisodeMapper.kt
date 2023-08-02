package tachiyomi.data.items.episode

import tachiyomi.domain.items.episode.model.Episode

// AM (FILLER)>
val episodeMapper: (Long, Long, String, String, String?, Boolean, Boolean, Boolean, Long, Long, Float, Long, Long, Long) -> Episode =
    { id, animeId, url, name, scanlator, seen, bookmark, fillermark, lastSecondSeen, totalSeconds, episodeNumber, sourceOrder, dateFetch, dateUpload ->
        Episode(
            id = id,
            animeId = animeId,
            seen = seen,
            bookmark = bookmark,
            // AM (FILLER) -->
            fillermark = fillermark,
            // <-- AM (FILLER)
            lastSecondSeen = lastSecondSeen,
            totalSeconds = totalSeconds,
            dateFetch = dateFetch,
            sourceOrder = sourceOrder,
            url = url,
            name = name,
            dateUpload = dateUpload,
            episodeNumber = episodeNumber,
            scanlator = scanlator,
        )
    }
