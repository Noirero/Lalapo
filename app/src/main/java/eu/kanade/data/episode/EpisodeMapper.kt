package eu.kanade.data.episode

import eu.kanade.domain.episode.model.Episode

val episodeMapper: (Long, Long, String, String, String?, Boolean, Boolean, Boolean, Long, Long, Float, Long, Long, Long) -> Episode =
    { id, animeId, url, name, scanlator, seen, bookmark, fillermark, lastSecondSeen, totalSeconds, episodeNumber, sourceOrder, dateFetch, dateUpload ->
        Episode(
            id = id,
            animeId = animeId,
            seen = seen,
            bookmark = bookmark,
            fillermark = fillermark,
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
