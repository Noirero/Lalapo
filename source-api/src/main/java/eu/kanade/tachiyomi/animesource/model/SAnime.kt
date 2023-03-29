package eu.kanade.tachiyomi.animesource.model

import eu.kanade.tachiyomi.source.model.UpdateStrategy
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

    var update_strategy: UpdateStrategy

    var initialized: Boolean

    fun getGenres(): List<String>? {
        if (genre.isNullOrBlank()) return null
        return genre?.split(", ")?.map { it.trim() }?.filterNot { it.isBlank() }?.distinct()
    }

    // AM (CU) -->
    val originalTitle: String
    val originalAuthor: String?
    val originalArtist: String?
    val originalDescription: String?
    val originalGenre: String?
    val originalStatus: Int
    // <-- AM (CU)

    fun copyFrom(other: SAnime) {
        // AM (CU) -->
        if (other.title.isNotBlank() && originalTitle != other.title) {
            title = other.originalTitle
        }
        // <-- AM (CU)

        if (other.author != null) {
            author = /* AM (CU) --> */ other.originalAuthor // <-- AM (CU)
        }

        if (other.artist != null) {
            artist = /* AM (CU) --> */ other.originalArtist // <-- AM (CU)
        }

        if (other.description != null) {
            description = /* AM (CU) --> */ other.originalDescription // <-- AM (CU)
        }

        if (other.genre != null) {
            genre = /* AM (CU) --> */ other.originalGenre // <-- AM (CU)
        }

        if (other.thumbnail_url != null) {
            thumbnail_url = other.thumbnail_url
        }

        status = other.status

        update_strategy = other.update_strategy

        if (!initialized) {
            initialized = other.initialized
        }
    }

    fun copy() = create().also {
        it.url = url
        // AM (CU) -->
        it.title = originalTitle
        it.artist = originalArtist
        it.author = originalAuthor
        it.description = originalDescription
        it.genre = originalGenre
        it.status = originalStatus
        // <-- AM (CU)
        it.thumbnail_url = thumbnail_url
        it.update_strategy = update_strategy
        it.initialized = initialized
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

// AM (CU) -->
fun SAnime.copy(
    url: String = this.url,
    title: String = this.originalTitle,
    artist: String? = this.originalArtist,
    author: String? = this.originalAuthor,
    description: String? = this.originalDescription,
    genre: String? = this.originalGenre,
    status: Int = this.status,
    thumbnail_url: String? = this.thumbnail_url,
    initialized: Boolean = this.initialized,
) = SAnime.create().also {
    it.url = url
    it.title = title
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = genre
    it.status = status
    it.thumbnail_url = thumbnail_url
    it.initialized = initialized
}
// <-- AM (CU)
