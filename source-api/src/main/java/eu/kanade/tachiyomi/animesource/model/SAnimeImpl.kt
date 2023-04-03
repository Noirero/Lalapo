package eu.kanade.tachiyomi.animesource.model

import eu.kanade.tachiyomi.source.model.UpdateStrategy

class SAnimeImpl : SAnime {

    override lateinit var url: String

    override lateinit var title: String

    override var artist: String? = null

    override var author: String? = null

    override var description: String? = null

    override var genre: String? = null

    override var status: Int = 0

    override var thumbnail_url: String? = null

    override var initialized: Boolean = false

    override var update_strategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE

    // AM (CU) -->
    override val originalTitle: String
        get() = title
    override val originalAuthor: String?
        get() = author
    override val originalArtist: String?
        get() = artist
    override val originalDescription: String?
        get() = description
    override val originalGenre: String?
        get() = genre
    override val originalStatus: Int
        get() = status
    // <-- AM (CU)
}
