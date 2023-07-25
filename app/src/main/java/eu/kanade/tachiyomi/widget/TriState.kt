package eu.kanade.tachiyomi.widget

import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import tachiyomi.domain.entries.TriStateFilter

fun Int.toTriStateFilter(): TriStateFilter {
    return when (this) {
        AnimeFilter.TriState.STATE_IGNORE -> TriStateFilter.DISABLED
        AnimeFilter.TriState.STATE_INCLUDE -> TriStateFilter.ENABLED_IS
        AnimeFilter.TriState.STATE_EXCLUDE -> TriStateFilter.ENABLED_NOT

        else -> throw IllegalStateException("Unknown TriState state: $this")
    }
}
