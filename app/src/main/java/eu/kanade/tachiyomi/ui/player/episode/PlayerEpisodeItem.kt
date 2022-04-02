package eu.kanade.tachiyomi.ui.player.episode

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.databinding.PlayerEpisodeItemBinding
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.setVectorCompat
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.Date

class PlayerEpisodeItem(episode: Episode, val anime: Anime, val isCurrent: Boolean, context: Context, val dateFormat: DateFormat, val decimalFormat: DecimalFormat) :
    AbstractFlexibleItem<PlayerEpisodeItem.ViewHolder>(),
    Episode by episode {

    val readColor = context.getResourceColor(R.attr.colorOnSurface, 0.38f)
    val unreadColor = context.getResourceColor(R.attr.colorOnSurface)
    val bookmarkedColor = context.getResourceColor(R.attr.colorAccent)
    val filleredColor = context.getResourceColor(R.attr.colorTertiary)

    override fun getLayoutRes(): Int {
        return R.layout.player_episode_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ViewHolder {
        return ViewHolder(view, adapter as PlayerEpisodeAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: ViewHolder,
        position: Int,
        payloads: List<Any?>?
    ) {
        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerEpisodeItem

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    inner class ViewHolder(view: View, private val adapter: PlayerEpisodeAdapter) : FlexibleViewHolder(view, adapter) {
        val binding = PlayerEpisodeItemBinding.bind(itemView)

        fun bind(item: PlayerEpisodeItem) {
            val anime = item.anime

            binding.episodeTitle.text = when (anime.displayMode) {
                Anime.EPISODE_DISPLAY_NUMBER -> {
                    val number = item.decimalFormat.format(item.episode_number.toDouble())
                    itemView.context.getString(R.string.display_mode_episode, number)
                }
                else -> item.name
            }

            // Set correct text color
            val episodeColor = when {
                item.seen -> item.readColor
                item.bookmark -> item.bookmarkedColor
                item.filler -> item.filleredColor
                else -> item.unreadColor
            }
            binding.episodeTitle.setTextColor(episodeColor)
            binding.episodeDetails.setTextColor(episodeColor)

            // bookmarkImage.isVisible = item.bookmark

            val descriptions = mutableListOf<CharSequence>()

            if (item.date_upload > 0) {
                descriptions.add(item.dateFormat.format(Date(item.date_upload)))
            }
            if (!item.scanlator.isNullOrBlank()) {
                descriptions.add(item.scanlator!!)
            }

            if (descriptions.isNotEmpty()) {
                binding.episodeDetails.text = descriptions.joinTo(SpannableStringBuilder(), " • ")
            } else {
                binding.episodeDetails.text = ""
            }

            if (item.bookmark) {
                binding.bookmarkImage.setVectorCompat(R.drawable.ic_bookmark_24dp, R.attr.colorAccent)
            } else {
                binding.bookmarkImage.setVectorCompat(R.drawable.ic_bookmark_border_24dp, R.attr.colorOnSurface)
            }

            if (item.filler) {
                binding.fillerImage.setVectorCompat(R.drawable.ic_filler_24dp, R.attr.colorTertiary)
            } else {
                binding.fillerImage.setVectorCompat(R.drawable.ic_filler_border_24dp, R.attr.colorOnSurface)
            }

            if (item.isCurrent) {
                binding.episodeTitle.setTypeface(null, Typeface.BOLD_ITALIC)
                binding.episodeDetails.setTypeface(null, Typeface.BOLD_ITALIC)
            } else {
                binding.episodeTitle.setTypeface(null, Typeface.NORMAL)
                binding.episodeDetails.setTypeface(null, Typeface.NORMAL)
            }
            binding.bookmarkLayout.setOnClickListener {
                adapter.clickBookmarkListener.bookmarkEpisode(item, item.anime)
            }
            binding.fillerLayout.setOnClickListener {
                adapter.clickFillerListener.fillerEpisode(item, item.anime)
            }
        }
    }
}
