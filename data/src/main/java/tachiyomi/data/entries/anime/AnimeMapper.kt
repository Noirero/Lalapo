package tachiyomi.data.entries.anime

import eu.kanade.tachiyomi.animesource.model.UpdateStrategy
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.anime.LibraryAnime

val animeMapper: (Long, Long, String, String?, String?, String?, List<String>?, String, Long, String?, Boolean, Long?, Long?, Boolean, Long, Long, Long, Long, UpdateStrategy, Long) -> Anime =
    { id, source, url, artist, author, description, genre, title, status, thumbnailUrl, favorite, lastUpdate, nextUpdate, initialized, viewerFlags, episodeFlags, coverLastModified, dateAdded, updateStrategy, calculateInterval ->
        Anime(
            id = id,
            source = source,
            favorite = favorite,
            lastUpdate = lastUpdate ?: 0,
            nextUpdate = nextUpdate ?: 0,
            calculateInterval = calculateInterval.toInt(),
            dateAdded = dateAdded,
            viewerFlags = viewerFlags,
            episodeFlags = episodeFlags,
            coverLastModified = coverLastModified,
            url = url,
            // AM (CU) -->
            ogTitle = title,
            ogArtist = artist,
            ogAuthor = author,
            ogDescription = description,
            ogGenre = genre,
            ogStatus = status,
            // <-- AM (CU)
            thumbnailUrl = thumbnailUrl,
            updateStrategy = updateStrategy,
            initialized = initialized,
        )
    }

val libraryAnime: (Long, Long, String, String?, String?, String?, List<String>?, String, Long, String?, Boolean, Long?, Long?, Boolean, Long, Long, Long, Long, UpdateStrategy, Long, Long, Long, Long, Long, Long, Long, Long, Long) -> LibraryAnime =
    { id, source, url, artist, author, description, genre, title, status, thumbnailUrl, favorite, lastUpdate, nextUpdate, initialized, viewerFlags, episodeFlags, coverLastModified, dateAdded, updateStrategy, calculateInterval, totalCount, seenCount, latestUpload, episodeFetchedAt, lastSeen, bookmarkCount, fillermarkCount, category ->
        LibraryAnime(
            anime = animeMapper(
                id,
                source,
                url,
                artist,
                author,
                description,
                genre,
                title,
                status,
                thumbnailUrl,
                favorite,
                lastUpdate,
                nextUpdate,
                initialized,
                viewerFlags,
                episodeFlags,
                coverLastModified,
                dateAdded,
                updateStrategy,
                calculateInterval,
            ),
            category = category,
            totalEpisodes = totalCount,
            seenCount = seenCount,
            bookmarkCount = bookmarkCount,
            // AM (FILLER) -->
            fillermarkCount = fillermarkCount,
            // <-- AM (FILLER)
            latestUpload = latestUpload,
            episodeFetchedAt = episodeFetchedAt,
            lastSeen = lastSeen,
        )
    }
