// AM (GU) -->
package eu.kanade.tachiyomi.data.track

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.bangumi.Bangumi
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import eu.kanade.tachiyomi.data.track.simkl.Simkl

enum class TrackStatus(val int: Int, @StringRes val res: Int) {
    WATCHING(1, R.string.watching),
    REPEATING(2, R.string.repeating_anime),
    PLAN_TO_WATCH(3, R.string.plan_to_watch),
    PAUSED(4, R.string.on_hold),
    COMPLETED(5, R.string.completed),
    DROPPED(6, R.string.dropped),
    OTHER(7, R.string.not_tracked),
    ;

    companion object {
        fun parseTrackerStatus(tracker: Long, statusLong: Long): TrackStatus? {
            val status = statusLong.toInt()
            return when (tracker) {
                TrackManager.MYANIMELIST -> {
                    when (status) {
                        MyAnimeList.WATCHING -> WATCHING
                        MyAnimeList.COMPLETED -> COMPLETED
                        MyAnimeList.ON_HOLD -> PAUSED
                        MyAnimeList.PLAN_TO_WATCH -> PLAN_TO_WATCH
                        MyAnimeList.DROPPED -> DROPPED
                        MyAnimeList.REWATCHING -> REPEATING
                        else -> null
                    }
                }
                TrackManager.ANILIST -> {
                    when (status) {
                        Anilist.WATCHING -> WATCHING
                        Anilist.COMPLETED -> COMPLETED
                        Anilist.PAUSED -> PAUSED
                        Anilist.PLANNING_ANIME -> PLAN_TO_WATCH
                        Anilist.DROPPED -> DROPPED
                        Anilist.REPEATING_ANIME -> REPEATING
                        else -> null
                    }
                }
                TrackManager.KITSU -> {
                    when (status) {
                        Kitsu.WATCHING -> WATCHING
                        Kitsu.COMPLETED -> COMPLETED
                        Kitsu.ON_HOLD -> PAUSED
                        Kitsu.PLAN_TO_WATCH -> PLAN_TO_WATCH
                        Kitsu.DROPPED -> DROPPED
                        else -> null
                    }
                }
                TrackManager.SHIKIMORI -> {
                    when (status) {
                        Shikimori.READING -> WATCHING
                        Shikimori.COMPLETED -> COMPLETED
                        Shikimori.ON_HOLD -> PAUSED
                        Shikimori.PLAN_TO_READ -> PLAN_TO_WATCH
                        Shikimori.DROPPED -> DROPPED
                        Shikimori.REREADING -> REPEATING
                        else -> null
                    }
                }
                TrackManager.BANGUMI -> {
                    when (status) {
                        Bangumi.READING -> WATCHING
                        Bangumi.COMPLETED -> COMPLETED
                        Bangumi.ON_HOLD -> PAUSED
                        Bangumi.PLAN_TO_READ -> PLAN_TO_WATCH
                        Bangumi.DROPPED -> DROPPED
                        else -> null
                    }
                }
                TrackManager.SIMKL -> {
                    when (status) {
                        Simkl.WATCHING -> WATCHING
                        Simkl.COMPLETED -> COMPLETED
                        Simkl.ON_HOLD -> PAUSED
                        Simkl.PLAN_TO_WATCH -> PLAN_TO_WATCH
                        Simkl.NOT_INTERESTING -> DROPPED
                        else -> null
                    }
                }
                else -> null
            }
        }
    }
}
// <-- AM (GU)
