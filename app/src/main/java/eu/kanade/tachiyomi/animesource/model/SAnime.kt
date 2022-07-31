package eu.kanade.tachiyomi.animesource.model

import dataanime.Animes
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeImpl
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import tachiyomi.animesource.model.AnimeInfo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.Serializable

interface SAnime : Serializable {

    var url: String

    var title: String

    var artist: String?

    var author: String?

    var description: String?

    var genre: String?

    var status: Int

    var thumbnail_url: String?

    var initialized: Boolean

    // AM -->
    val originalTitle: String
        get() = (this as? AnimeImpl)?.ogTitle ?: title
    val originalAuthor: String?
        get() = (this as? AnimeImpl)?.ogAuthor ?: author
    val originalArtist: String?
        get() = (this as? AnimeImpl)?.ogArtist ?: artist
    val originalDescription: String?
        get() = (this as? AnimeImpl)?.ogDesc ?: description
    val originalGenre: String?
        get() = (this as? AnimeImpl)?.ogGenre ?: genre
    val originalStatus: Int
        get() = (this as? AnimeImpl)?.ogStatus ?: status
    // AM <--

    fun copyFrom(other: SAnime) {
        // AM -->
        if (other.title.isNotBlank() && originalTitle != other.title) {
            val oldTitle = originalTitle
            title = other.originalTitle
            val source = (this as? Anime)?.source
            if (source != null) {
                Injekt.get<AnimeDownloadManager>().renameAnimeDir(oldTitle, other.originalTitle, source)
            }
        }
        // AM <--

        if (other.author != null) {
            author = /* AM --> */ other.originalAuthor // AM <--
        }

        if (other.artist != null) {
            artist = /* AM --> */ other.originalArtist // AM <--
        }

        if (other.description != null) {
            description = /* AM --> */ other.originalDescription // AM <--
        }

        if (other.genre != null) {
            genre = /* AM --> */ other.originalGenre // AM <--
        }

        if (other.thumbnail_url != null) {
            thumbnail_url = other.thumbnail_url
        }

        status = other.status

        if (!initialized) {
            initialized = other.initialized
        }
    }

    fun copyFrom(other: Animes) {
        if (other.author != null) {
            author = other.author
        }

        if (other.artist != null) {
            artist = /* AM --> */ other.artist // AM <--
        }

        if (other.description != null) {
            description = /* AM --> */ other.description // AM <--
        }

        if (other.genre != null) {
            genre = other.genre.joinToString(separator = ", ")
        }

        if (other.thumbnail_url != null) {
            thumbnail_url = other.thumbnail_url
        }

        status = other.status.toInt()

        if (!initialized) {
            initialized = other.initialized
        }
    }

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6

        fun create(): SAnime {
            return SAnimeImpl()
        }
    }
}

fun SAnime.toAnimeInfo(): AnimeInfo {
    return AnimeInfo(
        key = this.url,
        title = this.title,
        artist = this.artist ?: "",
        author = this.author ?: "",
        description = this.description ?: "",
        genres = this.genre?.split(", ") ?: emptyList(),
        status = this.status,
        cover = this.thumbnail_url ?: "",
    )
}

fun AnimeInfo.toSAnime(): SAnime {
    val animeInfo = this
    return SAnime.create().apply {
        url = animeInfo.key
        title = animeInfo.title
        artist = animeInfo.artist
        author = animeInfo.author
        description = animeInfo.description
        genre = animeInfo.genres.joinToString(", ")
        status = animeInfo.status
        thumbnail_url = animeInfo.cover
    }
}
