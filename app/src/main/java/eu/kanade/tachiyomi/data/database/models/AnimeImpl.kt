package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.data.animelib.CustomAnimeManager
import uy.kohesive.injekt.injectLazy

open class AnimeImpl : Anime {

    override var id: Long? = null

    override var source: Long = -1

    override lateinit var url: String

    override var title: String
        get() = if (favorite) {
            val customTitle = customAnimeManager.getAnime(this)?.title
            if (customTitle.isNullOrBlank()) ogTitle else customTitle
        } else {
            ogTitle
        }
        set(value) {
            ogTitle = value
        }

    override var author: String?
        get() = if (favorite) customAnimeManager.getAnime(this)?.author ?: ogAuthor else ogAuthor
        set(value) { ogAuthor = value }

    override var artist: String?
        get() = if (favorite) customAnimeManager.getAnime(this)?.artist ?: ogArtist else ogArtist
        set(value) { ogArtist = value }

    override var description: String?
        get() = if (favorite) customAnimeManager.getAnime(this)?.description ?: ogDesc else ogDesc
        set(value) { ogDesc = value }

    override var genre: String?
        get() = if (favorite) customAnimeManager.getAnime(this)?.genre ?: ogGenre else ogGenre
        set(value) { ogGenre = value }

    override var status: Int
        get() = if (favorite) customAnimeManager.getAnime(this)?.status?.takeUnless { it == 0 } ?: ogStatus else ogStatus
        set(value) { ogStatus = value }

    override var thumbnail_url: String? = null

    override var favorite: Boolean = false

    override var last_update: Long = 0

    override var date_added: Long = 0

    override var initialized: Boolean = false

    override var viewer_flags: Int = 0

    override var episode_flags: Int = 0

    override var cover_last_modified: Long = 0

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

    companion object {
        private val customAnimeManager: CustomAnimeManager by injectLazy()
    }
}
