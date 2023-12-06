package eu.kanade.tachiyomi.ui.stats

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.stats.AnimeStatsScreenContent
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.tachiyomi.ui.stats.anime.AnimeStatsScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

class StatsScreen : Screen {

    override val key = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { AnimeStatsScreenModel() }
        val state by screenModel.state.collectAsState()

        if (state is StatsScreenState.Loading) {
            LoadingScreen()
            return
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.label_stats),
                    navigateUp = navigator::pop,
                )
            },
        ) { paddingValues ->
            AnimeStatsScreenContent(
                state = state as StatsScreenState.SuccessAnime,
                paddingValues = paddingValues,
            )
        }
    }
}
