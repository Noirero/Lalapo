package eu.kanade.tachiyomi.ui.setting.database

import android.os.Bundle
import eu.kanade.domain.animesource.interactor.GetAnimeSourcesWithNonLibraryAnime
import eu.kanade.tachiyomi.mi.AnimeDatabase
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.flow.collectLatest
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ClearDatabasePresenter(
    private val animedatabase: AnimeDatabase = Injekt.get(),
    private val getAnimeSourcesWithNonLibraryAnime: GetAnimeSourcesWithNonLibraryAnime = Injekt.get(),
) : BasePresenter<ClearDatabaseController>() {

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        presenterScope.launchIO {
            getAnimeSourcesWithNonLibraryAnime.subscribe()
                .collectLatest { list ->
                    val items = list
                        .map { (source, count) -> ClearDatabaseAnimeSourceItem(source, count) }
                        .sortedBy { it.source.name }

                    withUIContext { view?.setItemsAnime(items) }
                }
        }
    }

    fun clearDatabaseForAnimeSourceIds(animeSources: List<Long>) {
        animedatabase.animesQueries.deleteAnimesNotInLibraryBySourceIds(animeSources)
        animedatabase.animehistoryQueries.removeResettedHistory()
    }
}
