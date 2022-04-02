package eu.kanade.tachiyomi.ui.player.episode

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode

class PlayerEpisodeAdapter(
    bookmarkDialog: OnBookmarkClickListener,
    fillerDialog: OnFillerClickListener
) : FlexibleAdapter<PlayerEpisodeItem>(null, bookmarkDialog, true) {

    /**
     * Listener for browse item clicks.
     */
    val clickBookmarkListener: OnBookmarkClickListener = bookmarkDialog

    /**
     * Listener which should be called when user clicks the download icons.
     */
    interface OnBookmarkClickListener {
        fun bookmarkEpisode(episode: Episode, anime: Anime)
    }

    /**
     * Listener for browse item clicks.
     */
    val clickFillerListener: OnFillerClickListener = fillerDialog

    /**
     * Listener which should be called when user clicks the download icons.
     */
    interface OnFillerClickListener {
        fun fillerEpisode(episode: Episode, anime: Anime)
    }
}
