// AM (BROWSE) -->
package eu.kanade.tachiyomi.ui.browse.anime.migration.sources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.anime.MigrateAnimeSourceScreen
import eu.kanade.tachiyomi.ui.browse.anime.migration.anime.MigrateAnimeScreen

class MigrateAnimeSourceScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MigrateAnimeSourceScreenModel() }
        val state by screenModel.state.collectAsState()

        MigrateAnimeSourceScreen(
            state = state,
            navigateUp = navigator::pop,
            onClickItem = { source -> navigator.push(MigrateAnimeScreen(source.id)) },
            onToggleSortingDirection = screenModel::toggleSortingDirection,
            onToggleSortingMode = screenModel::toggleSortingMode,
        )
    }
}
// <-- AM (BROWSE)
