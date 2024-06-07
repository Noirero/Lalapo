// AM (RECENTS) -->
package eu.kanade.tachiyomi.ui.recents

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connection.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connection.discord.DiscordScreen
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.anime.AnimeHistoryScreenModel
import eu.kanade.tachiyomi.ui.history.anime.animeHistoryTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.updates.anime.animeUpdatesTab
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.core.util.lang.launchIO
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data class RecentsTab(
    private val toHistory: Boolean,
) : Tab() {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_recents_enter)
            return TabOptions(
                index = 1u,
                title = stringResource(MR.strings.label_recent_recents),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        // AM (REMOVE_TABBED_SCREENS) -->
        navigator.push(DownloadQueueScreen)
        // <-- AM (REMOVE_TABBED_SCREENS)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val animeHistoryScreenModel = rememberScreenModel { AnimeHistoryScreenModel() }
        val animeSearchQuery by animeHistoryScreenModel.query.collectAsState()

        TabbedScreen(
            titleRes = MR.strings.label_recent_recents,
            tabs = persistentListOf(
                animeUpdatesTab(context),
                animeHistoryTab(context),
            ),
            startIndex = 1.takeIf { toHistory },
            // Compatibility with hardcoded aniyomi code
            mangaSearchQuery = animeSearchQuery,
            onChangeMangaSearchQuery = animeHistoryScreenModel::search,
        )

        LaunchedEffect(Unit) {
            // AM (DISCORD) -->
            with(DiscordRPCService) {
                discordScope.launchIO { setScreen(context.applicationContext, DiscordScreen.RECENTS) }
            }
            // <-- AM (DISCORD)
            (context as? MainActivity)?.ready = true
        }
    }
}
// <-- AM (RECENTS)
