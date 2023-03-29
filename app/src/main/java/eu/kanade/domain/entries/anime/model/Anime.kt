package eu.kanade.domain.entries.anime.model

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.entries.TriStateFilter
import eu.kanade.domain.entries.anime.interactor.GetCustomAnimeInfo
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.source.anime.LocalAnimeSource
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.Serializable

data class Anime(
    val id: Long,
    val source: Long,
    val favorite: Boolean,
    val lastUpdate: Long,
    val dateAdded: Long,
    val viewerFlags: Long,
    val episodeFlags: Long,
    val coverLastModified: Long,
    val url: String,
    // AM (CU) -->
    val ogTitle: String,
    val ogArtist: String?,
    val ogAuthor: String?,
    val ogDescription: String?,
    val ogGenre: List<String>?,
    val ogStatus: Long,
    // <-- AM (CU)
    val thumbnailUrl: String?,
    val updateStrategy: UpdateStrategy,
    val initialized: Boolean,
) : Serializable {

    // AM (CU) -->
    private val customAnimeInfo = if (favorite) {
        getCustomAnimeInfo.get(id)
    } else {
        null
    }

    val title: String
        get() = customAnimeInfo?.title ?: ogTitle

    val author: String?
        get() = customAnimeInfo?.author ?: ogAuthor

    val artist: String?
        get() = customAnimeInfo?.artist ?: ogArtist

    val description: String?
        get() = customAnimeInfo?.description ?: ogDescription

    val genre: List<String>?
        get() = customAnimeInfo?.genre ?: ogGenre

    val status: Long
        get() = customAnimeInfo?.status ?: ogStatus
    // <-- AM (CU)

    val sorting: Long
        get() = episodeFlags and EPISODE_SORTING_MASK

    val displayMode: Long
        get() = episodeFlags and EPISODE_DISPLAY_MASK

    val unseenFilterRaw: Long
        get() = episodeFlags and EPISODE_UNSEEN_MASK

    val downloadedFilterRaw: Long
        get() = episodeFlags and EPISODE_DOWNLOADED_MASK

    val bookmarkedFilterRaw: Long
        get() = episodeFlags and EPISODE_BOOKMARKED_MASK

    // AM (FM) -->
    val fillermarkedFilterRaw: Long
        get() = episodeFlags and EPISODE_FILLERMARKED_MASK
    // <-- AM (FM)

    val skipIntroLength: Long
        get() = viewerFlags

    val unseenFilter: TriStateFilter
        get() = when (unseenFilterRaw) {
            EPISODE_SHOW_UNSEEN -> TriStateFilter.ENABLED_IS
            EPISODE_SHOW_SEEN -> TriStateFilter.ENABLED_NOT
            else -> TriStateFilter.DISABLED
        }

    val downloadedFilter: TriStateFilter
        get() {
            if (forceDownloaded()) return TriStateFilter.ENABLED_IS
            return when (downloadedFilterRaw) {
                EPISODE_SHOW_DOWNLOADED -> TriStateFilter.ENABLED_IS
                EPISODE_SHOW_NOT_DOWNLOADED -> TriStateFilter.ENABLED_NOT
                else -> TriStateFilter.DISABLED
            }
        }

    val bookmarkedFilter: TriStateFilter
        get() = when (bookmarkedFilterRaw) {
            EPISODE_SHOW_BOOKMARKED -> TriStateFilter.ENABLED_IS
            EPISODE_SHOW_NOT_BOOKMARKED -> TriStateFilter.ENABLED_NOT
            else -> TriStateFilter.DISABLED
        }

    // AM (FM) -->
    val fillermarkedFilter: TriStateFilter
        get() = when (fillermarkedFilterRaw) {
            EPISODE_SHOW_FILLERMARKED -> TriStateFilter.ENABLED_IS
            EPISODE_SHOW_NOT_FILLERMARKED -> TriStateFilter.ENABLED_NOT
            else -> TriStateFilter.DISABLED
        }
    // <-- AM (FM)

    fun episodesFiltered(): Boolean {
        return unseenFilter != TriStateFilter.DISABLED ||
            downloadedFilter != TriStateFilter.DISABLED ||
            bookmarkedFilter != TriStateFilter.DISABLED ||
            // AM (FM) -->
            fillermarkedFilter != TriStateFilter.DISABLED
        // <-- AM (FM)
    }

    fun forceDownloaded(): Boolean {
        return favorite && Injekt.get<BasePreferences>().downloadedOnly().get()
    }

    fun sortDescending(): Boolean {
        return episodeFlags and EPISODE_SORT_DIR_MASK == EPISODE_SORT_DESC
    }

    fun toSAnime(): SAnime = SAnime.create().also {
        it.url = url
        it.title = title
        it.artist = artist
        it.author = author
        it.description = description
        it.genre = genre.orEmpty().joinToString()
        it.status = status.toInt()
        it.thumbnail_url = thumbnailUrl
        it.initialized = initialized
    }

    fun copyFrom(other: SAnime): Anime {
        val author = other.author ?: author
        val artist = other.artist ?: artist
        val description = other.description ?: description
        val genres = if (other.genre != null) {
            other.getGenres()
        } else {
            genre
        }
        val thumbnailUrl = other.thumbnail_url ?: thumbnailUrl
        return this.copy(
            // AM (CU) -->
            ogAuthor = author,
            ogArtist = artist,
            ogDescription = description,
            ogGenre = genres,
            // <-- AM (CU)
            thumbnailUrl = thumbnailUrl,
            // AM (CU) -->
            ogStatus = other.status.toLong(),
            // <-- AM (CU)
            updateStrategy = other.update_strategy,
            initialized = other.initialized && initialized,
        )
    }

    companion object {
        // Generic filter that does not filter anything
        const val SHOW_ALL = 0x00000000L

        const val EPISODE_SORT_DESC = 0x00000000L
        const val EPISODE_SORT_ASC = 0x00000001L
        const val EPISODE_SORT_DIR_MASK = 0x00000001L

        const val EPISODE_SHOW_UNSEEN = 0x00000002L
        const val EPISODE_SHOW_SEEN = 0x00000004L
        const val EPISODE_UNSEEN_MASK = 0x00000006L

        const val EPISODE_SHOW_DOWNLOADED = 0x00000008L
        const val EPISODE_SHOW_NOT_DOWNLOADED = 0x00000010L
        const val EPISODE_DOWNLOADED_MASK = 0x00000018L

        const val EPISODE_SHOW_BOOKMARKED = 0x00000020L
        const val EPISODE_SHOW_NOT_BOOKMARKED = 0x00000040L
        const val EPISODE_BOOKMARKED_MASK = 0x00000060L

        // AM (FM) -->
        const val EPISODE_SHOW_FILLERMARKED = 0x00000080L
        const val EPISODE_SHOW_NOT_FILLERMARKED = 0x00000100L
        const val EPISODE_FILLERMARKED_MASK = 0x00000180L

        const val EPISODE_SORTING_SOURCE = 0x00000000L
        const val EPISODE_SORTING_NUMBER = 0x00000200L
        const val EPISODE_SORTING_UPLOAD_DATE = 0x00000400L
        const val EPISODE_SORTING_MASK = 0x00000600L
        // <-- AM (FM)

        const val EPISODE_DISPLAY_NAME = 0x00000000L
        const val EPISODE_DISPLAY_NUMBER = 0x00100000L
        const val EPISODE_DISPLAY_MASK = 0x00100000L

        fun create() = Anime(
            id = -1L,
            url = "",
            // AM (CU) -->
            ogTitle = "",
            // <-- AM (CU)
            source = -1L,
            favorite = false,
            lastUpdate = 0L,
            dateAdded = 0L,
            viewerFlags = 0L,
            episodeFlags = 0L,
            coverLastModified = 0L,
            // AM (CU) -->
            ogArtist = null,
            ogAuthor = null,
            ogDescription = null,
            ogGenre = null,
            ogStatus = 0L,
            // <-- AM (CU)
            thumbnailUrl = null,
            updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
            initialized = false,
        )

        // AM (CU) -->
        private val getCustomAnimeInfo: GetCustomAnimeInfo by injectLazy()
        // <-- AM (CU)
    }
}

fun Anime.toAnimeUpdate(): AnimeUpdate {
    return AnimeUpdate(
        id = id,
        source = source,
        favorite = favorite,
        lastUpdate = lastUpdate,
        dateAdded = dateAdded,
        viewerFlags = viewerFlags,
        episodeFlags = episodeFlags,
        coverLastModified = coverLastModified,
        // AM (CU) -->
        title = ogTitle,
        artist = ogArtist,
        author = ogAuthor,
        description = ogDescription,
        genre = ogGenre,
        status = ogStatus,
        // <-- AM (CU)
        thumbnailUrl = thumbnailUrl,
        updateStrategy = updateStrategy,
        initialized = initialized,
    )
}

fun SAnime.toDomainAnime(sourceId: Long): Anime {
    return Anime.create().copy(
        url = url,
        // AM (CU) -->
        ogTitle = title,
        ogArtist = artist,
        ogAuthor = author,
        ogDescription = description,
        ogGenre = getGenres(),
        ogStatus = status.toLong(),
        // <-- AM (CU)
        thumbnailUrl = thumbnail_url,
        updateStrategy = update_strategy,
        initialized = initialized,
        source = sourceId,
    )
}

fun Anime.isLocal(): Boolean = source == LocalAnimeSource.ID

fun Anime.hasCustomCover(coverCache: AnimeCoverCache = Injekt.get()): Boolean {
    return coverCache.getCustomCoverFile(id).exists()
}
