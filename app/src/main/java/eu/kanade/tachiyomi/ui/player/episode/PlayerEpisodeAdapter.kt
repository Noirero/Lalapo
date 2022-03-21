package eu.kanade.tachiyomi.ui.player.episode

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode

class PlayerEpisodeAdapter(
    dialog: OnBookmarkClickListener
) : FlexibleAdapter<PlayerEpisodeItem>(null, dialog, true) {

    /**
     * Listener for browse item clicks.
     */
    val clickListener: OnBookmarkClickListener = dialog

    /**
     * Listener which should be called when user clicks the download icons.
     */
    interface OnBookmarkClickListener {
        fun bookmarkEpisode(episode: Episode, anime: Anime)
    }
}
