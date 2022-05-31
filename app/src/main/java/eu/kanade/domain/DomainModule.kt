package eu.kanade.domain

import eu.kanade.data.anime.AnimeRepositoryImpl
import eu.kanade.data.animehistory.AnimeHistoryRepositoryImpl
import eu.kanade.data.animesource.AnimeSourceRepositoryImpl
import eu.kanade.data.episode.EpisodeRepositoryImpl
import eu.kanade.domain.anime.repository.AnimeRepository
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensionLanguages
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensionSources
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensionUpdates
import eu.kanade.domain.animeextension.interactor.GetAnimeExtensions
import eu.kanade.domain.animehistory.interactor.DeleteAnimeHistoryTable
import eu.kanade.domain.animehistory.interactor.GetAnimeHistory
import eu.kanade.domain.animehistory.interactor.GetNextEpisodeForAnime
import eu.kanade.domain.animehistory.interactor.RemoveAnimeHistoryByAnimeId
import eu.kanade.domain.animehistory.interactor.RemoveAnimeHistoryById
import eu.kanade.domain.animehistory.interactor.UpsertAnimeHistory
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository
import eu.kanade.domain.animesource.interactor.GetAnimeSourcesWithFavoriteCount
import eu.kanade.domain.animesource.interactor.GetEnabledAnimeSources
import eu.kanade.domain.animesource.interactor.GetLanguagesWithAnimeSources
import eu.kanade.domain.animesource.interactor.SetMigrateSorting
import eu.kanade.domain.animesource.interactor.ToggleAnimeSource
import eu.kanade.domain.animesource.interactor.ToggleAnimeSourcePin
import eu.kanade.domain.animesource.interactor.ToggleLanguage
import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import eu.kanade.domain.episode.interactor.UpdateEpisode
import eu.kanade.domain.episode.repository.EpisodeRepository
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get
import eu.kanade.domain.anime.interactor.GetFavoritesBySourceId as GetFavoritesBySourceIdAnime
import eu.kanade.domain.anime.interactor.ResetViewerFlags as ResetViewerFlagsAnime

class DomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<AnimeRepository> { AnimeRepositoryImpl(get()) }
        addFactory { GetFavoritesBySourceIdAnime(get()) }
        addFactory { GetNextEpisodeForAnime(get()) }
        addFactory { ResetViewerFlagsAnime(get()) }

        addSingletonFactory<EpisodeRepository> { EpisodeRepositoryImpl(get()) }
        addFactory { UpdateEpisode(get()) }

        addSingletonFactory<AnimeHistoryRepository> { AnimeHistoryRepositoryImpl(get()) }
        addFactory { DeleteAnimeHistoryTable(get()) }
        addFactory { GetAnimeHistory(get()) }
        addFactory { UpsertAnimeHistory(get()) }
        addFactory { RemoveAnimeHistoryById(get()) }
        addFactory { RemoveAnimeHistoryByAnimeId(get()) }

        addSingletonFactory<AnimeSourceRepository> { AnimeSourceRepositoryImpl(get(), get()) }
        addFactory { GetLanguagesWithAnimeSources(get(), get()) }
        addFactory { GetEnabledAnimeSources(get(), get()) }
        addFactory { ToggleAnimeSource(get()) }
        addFactory { ToggleAnimeSourcePin(get()) }
        addFactory { GetAnimeSourcesWithFavoriteCount(get(), get()) }

        addFactory { GetAnimeExtensions(get(), get()) }
        addFactory { GetAnimeExtensionSources(get()) }
        addFactory { GetAnimeExtensionUpdates(get(), get()) }
        addFactory { GetAnimeExtensionLanguages(get(), get()) }
        addFactory { SetMigrateSorting(get()) }
        addFactory { ToggleLanguage(get()) }
    }
}
