// AY -->
package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.home.HomeScreen
import tachiyomi.i18n.MR
import tachiyomi.i18n.animiru.AMMR

enum class StartScreen(val titleRes: StringResource, val tab: HomeScreen.Tab) {
    ANIME(MR.strings.label_library, HomeScreen.Tab.Library()),

    // AM (RECENTS) -->
    UPDATES(MR.strings.label_recent_updates, HomeScreen.Tab.Recents(toHistory = false)),
    HISTORY(MR.strings.label_recent_manga, HomeScreen.Tab.Recents(toHistory = true)),

    // <-- AM (RECENTS)
    // AM (BROWSE) -->
    BROWSE(AMMR.strings.am_label_discover, HomeScreen.Tab.Browse(toExtensions = false)),
    // <-- AM (BROWSE)
}
// <-- AY
