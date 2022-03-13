package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.data.database.models.AnimeTrack

fun AnimeTrack.toBangumiStatus() = when (status) {
    Bangumi.WATCHING -> "do"
    Bangumi.COMPLETED -> "collect"
    Bangumi.ON_HOLD -> "on_hold"
    Bangumi.DROPPED -> "dropped"
    Bangumi.PLANNING -> "wish"
    else -> throw NotImplementedError("Unknown status: $status")
}

fun toTrackStatus(status: String) = when (status) {
    "do" -> Bangumi.WATCHING
    "collect" -> Bangumi.COMPLETED
    "on_hold" -> Bangumi.ON_HOLD
    "dropped" -> Bangumi.DROPPED
    "wish" -> Bangumi.PLANNING
    else -> throw NotImplementedError("Unknown status: $status")
}
