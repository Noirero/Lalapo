package eu.kanade.domain.items.episode.model

data class EpisodeUpdate(
    val id: Long,
    val animeId: Long? = null,
    val seen: Boolean? = null,
    val bookmark: Boolean? = null,
    // AM (FM) -->
    val fillermark: Boolean? = null,
    // AM (FM) <--
    val lastSecondSeen: Long? = null,
    val totalSeconds: Long? = null,
    val dateFetch: Long? = null,
    val sourceOrder: Long? = null,
    val url: String? = null,
    val name: String? = null,
    val dateUpload: Long? = null,
    val episodeNumber: Float? = null,
    val scanlator: String? = null,
)

fun Episode.toEpisodeUpdate(): EpisodeUpdate {
    // AM (FM)>
    return EpisodeUpdate(id, animeId, seen, bookmark, fillermark, lastSecondSeen, totalSeconds, dateFetch, sourceOrder, url, name, dateUpload, episodeNumber, scanlator)
}
