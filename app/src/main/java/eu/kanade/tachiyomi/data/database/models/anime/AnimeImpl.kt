package eu.kanade.tachiyomi.data.database.models.anime

import eu.kanade.tachiyomi.data.library.anime.CustomAnimeManager
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import uy.kohesive.injekt.injectLazy

open class AnimeImpl : Anime {

    override var id: Long? = null

    override var source: Long = -1

    override lateinit var url: String

    // AM (CU) -->
    private val customAnime: CustomAnimeManager.CustomAnimeInfo?
        get() = customAnimeManager.getAnime(this)

    override var title: String
        get() = if (favorite) {
            customAnime?.title ?: ogTitle
        } else {
            ogTitle
        }
        set(value) {
            ogTitle = value
        }

    override var author: String?
        get() = if (favorite) customAnime?.author ?: ogAuthor else ogAuthor
        set(value) { ogAuthor = value }

    override var artist: String?
        get() = if (favorite) customAnime?.artist ?: ogArtist else ogArtist
        set(value) { ogArtist = value }

    override var description: String?
        get() = if (favorite) customAnime?.description ?: ogDesc else ogDesc
        set(value) { ogDesc = value }

    override var genre: String?
        get() = if (favorite) customAnime?.genre?.joinToString() ?: ogGenre else ogGenre
        set(value) { ogGenre = value }

    override var status: Int
        get() = if (favorite) customAnime?.status?.toInt() ?: ogStatus else ogStatus
        set(value) { ogStatus = value }
    // <-- AM (CU)

    override var thumbnail_url: String? = null

    override var favorite: Boolean = false

    override var last_update: Long = 0

    override var date_added: Long = 0

    override var update_strategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE

    override var initialized: Boolean = false

    // AM (CU) -->
    lateinit var ogTitle: String
        private set
    var ogAuthor: String? = null
        private set
    var ogArtist: String? = null
        private set
    var ogDesc: String? = null
        private set
    var ogGenre: String? = null
        private set
    var ogStatus: Int = 0
        private set

    override val originalTitle: String
        get() = ogTitle
    override val originalAuthor: String?
        get() = ogAuthor ?: author
    override val originalArtist: String?
        get() = ogArtist ?: artist
    override val originalDescription: String?
        get() = ogDesc ?: description
    override val originalGenre: String?
        get() = ogGenre ?: genre
    override val originalStatus: Int
        get() = ogStatus
    // <-- AM (CU)

    override var viewer_flags: Int = 0

    override var episode_flags: Int = 0

    override var cover_last_modified: Long = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val anime = other as Anime
        if (url != anime.url) return false
        return id == anime.id
    }

    override fun hashCode(): Int {
        return url.hashCode() + id.hashCode()
    }

    // AM (CU) -->
    companion object {
        private val customAnimeManager: CustomAnimeManager by injectLazy()
    }
    // <-- AM (CU)
}
