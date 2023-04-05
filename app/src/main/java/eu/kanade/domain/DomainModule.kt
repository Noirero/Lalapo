package eu.kanade.domain

import android.app.Application
import eu.kanade.data.category.anime.AnimeCategoryRepositoryImpl
import eu.kanade.data.entries.anime.AnimeRepositoryImpl
import eu.kanade.data.entries.anime.CustomAnimeRepositoryImpl
import eu.kanade.data.history.anime.AnimeHistoryRepositoryImpl
import eu.kanade.data.items.episode.EpisodeRepositoryImpl
import eu.kanade.data.source.anime.AnimeSourceDataRepositoryImpl
import eu.kanade.data.source.anime.AnimeSourceRepositoryImpl
import eu.kanade.data.track.anime.AnimeTrackRepositoryImpl
import eu.kanade.data.updates.anime.AnimeUpdatesRepositoryImpl
import eu.kanade.domain.category.anime.interactor.CreateAnimeCategoryWithName
import eu.kanade.domain.category.anime.interactor.DeleteAnimeCategory
import eu.kanade.domain.category.anime.interactor.GetAnimeCategories
import eu.kanade.domain.category.anime.interactor.RenameAnimeCategory
import eu.kanade.domain.category.anime.interactor.ReorderAnimeCategory
import eu.kanade.domain.category.anime.interactor.ResetAnimeCategoryFlags
import eu.kanade.domain.category.anime.interactor.SetAnimeCategories
import eu.kanade.domain.category.anime.interactor.SetDisplayModeForAnimeCategory
import eu.kanade.domain.category.anime.interactor.SetSortModeForAnimeCategory
import eu.kanade.domain.category.anime.interactor.UpdateAnimeCategory
import eu.kanade.domain.category.anime.repository.AnimeCategoryRepository
import eu.kanade.domain.download.anime.interactor.DeleteAnimeDownload
import eu.kanade.domain.entries.anime.interactor.GetAllAnime
import eu.kanade.domain.entries.anime.interactor.GetAnime
import eu.kanade.domain.entries.anime.interactor.GetAnimeFavorites
import eu.kanade.domain.entries.anime.interactor.GetAnimeWithEpisodes
import eu.kanade.domain.entries.anime.interactor.GetCustomAnimeInfo
import eu.kanade.domain.entries.anime.interactor.GetDuplicateLibraryAnime
import eu.kanade.domain.entries.anime.interactor.GetLibraryAnime
import eu.kanade.domain.entries.anime.interactor.NetworkToLocalAnime
import eu.kanade.domain.entries.anime.interactor.ResetAnimeViewerFlags
import eu.kanade.domain.entries.anime.interactor.SetAnimeEpisodeFlags
import eu.kanade.domain.entries.anime.interactor.SetAnimeViewerFlags
import eu.kanade.domain.entries.anime.interactor.SetCustomAnimeInfo
import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.repository.AnimeRepository
import eu.kanade.domain.entries.anime.repository.CustomAnimeRepository
import eu.kanade.domain.extension.anime.interactor.GetAnimeExtensionLanguages
import eu.kanade.domain.extension.anime.interactor.GetAnimeExtensionSources
import eu.kanade.domain.extension.anime.interactor.GetAnimeExtensionsByType
import eu.kanade.domain.history.anime.interactor.GetAnimeHistory
import eu.kanade.domain.history.anime.interactor.GetNextEpisodes
import eu.kanade.domain.history.anime.interactor.RemoveAnimeHistory
import eu.kanade.domain.history.anime.interactor.UpsertAnimeHistory
import eu.kanade.domain.history.anime.repository.AnimeHistoryRepository
import eu.kanade.domain.items.episode.interactor.GetEpisode
import eu.kanade.domain.items.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.items.episode.interactor.SetAnimeDefaultEpisodeFlags
import eu.kanade.domain.items.episode.interactor.SetSeenStatus
import eu.kanade.domain.items.episode.interactor.ShouldUpdateDbEpisode
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.items.episode.interactor.SyncEpisodesWithTrackServiceTwoWay
import eu.kanade.domain.items.episode.interactor.UpdateEpisode
import eu.kanade.domain.items.episode.repository.EpisodeRepository
import eu.kanade.domain.source.anime.interactor.GetAnimeSourcesWithFavoriteCount
import eu.kanade.domain.source.anime.interactor.GetAnimeSourcesWithNonLibraryAnime
import eu.kanade.domain.source.anime.interactor.GetEnabledAnimeSources
import eu.kanade.domain.source.anime.interactor.GetLanguagesWithAnimeSources
import eu.kanade.domain.source.anime.interactor.GetRemoteAnime
import eu.kanade.domain.source.anime.interactor.ToggleAnimeSource
import eu.kanade.domain.source.anime.interactor.ToggleAnimeSourcePin
import eu.kanade.domain.source.anime.repository.AnimeSourceDataRepository
import eu.kanade.domain.source.anime.repository.AnimeSourceRepository
import eu.kanade.domain.source.service.SetMigrateSorting
import eu.kanade.domain.source.service.ToggleLanguage
import eu.kanade.domain.track.anime.interactor.DeleteAnimeTrack
import eu.kanade.domain.track.anime.interactor.GetAnimeTracks
import eu.kanade.domain.track.anime.interactor.GetTracksPerAnime
import eu.kanade.domain.track.anime.interactor.InsertAnimeTrack
import eu.kanade.domain.track.anime.repository.AnimeTrackRepository
import eu.kanade.domain.updates.anime.interactor.GetAnimeUpdates
import eu.kanade.domain.updates.anime.repository.AnimeUpdatesRepository
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class DomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<AnimeCategoryRepository> { AnimeCategoryRepositoryImpl(get()) }
        addFactory { GetAnimeCategories(get()) }
        addFactory { ResetAnimeCategoryFlags(get(), get()) }
        addFactory { SetDisplayModeForAnimeCategory(get(), get()) }
        addFactory { SetSortModeForAnimeCategory(get(), get()) }
        addFactory { CreateAnimeCategoryWithName(get(), get()) }
        addFactory { RenameAnimeCategory(get()) }
        addFactory { ReorderAnimeCategory(get()) }
        addFactory { UpdateAnimeCategory(get()) }
        addFactory { DeleteAnimeCategory(get()) }

        addSingletonFactory<AnimeRepository> { AnimeRepositoryImpl(get()) }
        addFactory { GetDuplicateLibraryAnime(get()) }
        addFactory { GetAnimeFavorites(get()) }
        addFactory { GetLibraryAnime(get()) }
        addFactory { GetAnimeWithEpisodes(get(), get()) }
        addFactory { GetAnime(get()) }
        addFactory { GetNextEpisodes(get(), get(), get()) }
        addFactory { ResetAnimeViewerFlags(get()) }
        addFactory { SetAnimeEpisodeFlags(get()) }
        addFactory { SetAnimeDefaultEpisodeFlags(get(), get(), get()) }
        addFactory { SetAnimeViewerFlags(get()) }
        addFactory { NetworkToLocalAnime(get()) }
        addFactory { UpdateAnime(get()) }
        addFactory { SetAnimeCategories(get()) }

        addSingletonFactory<AnimeTrackRepository> { AnimeTrackRepositoryImpl(get()) }
        addFactory { DeleteAnimeTrack(get()) }
        addFactory { GetTracksPerAnime(get()) }
        addFactory { GetAnimeTracks(get()) }
        addFactory { InsertAnimeTrack(get()) }

        addSingletonFactory<EpisodeRepository> { EpisodeRepositoryImpl(get()) }
        addFactory { GetEpisode(get()) }
        addFactory { GetEpisodeByAnimeId(get()) }
        addFactory { UpdateEpisode(get()) }
        addFactory { SetSeenStatus(get(), get(), get(), get()) }
        addFactory { ShouldUpdateDbEpisode() }
        addFactory { SyncEpisodesWithSource(get(), get(), get(), get()) }
        addFactory { SyncEpisodesWithTrackServiceTwoWay(get(), get()) }

        addSingletonFactory<AnimeHistoryRepository> { AnimeHistoryRepositoryImpl(get()) }
        addFactory { GetAnimeHistory(get()) }
        addFactory { UpsertAnimeHistory(get()) }
        addFactory { RemoveAnimeHistory(get()) }

        addFactory { DeleteAnimeDownload(get(), get()) }

        addFactory { GetAnimeExtensionsByType(get(), get()) }
        addFactory { GetAnimeExtensionSources(get()) }
        addFactory { GetAnimeExtensionLanguages(get(), get()) }

        addSingletonFactory<AnimeUpdatesRepository> { AnimeUpdatesRepositoryImpl(get()) }
        addFactory { GetAnimeUpdates(get()) }

        addSingletonFactory<AnimeSourceRepository> { AnimeSourceRepositoryImpl(get(), get()) }
        addSingletonFactory<AnimeSourceDataRepository> { AnimeSourceDataRepositoryImpl(get()) }
        addFactory { GetEnabledAnimeSources(get(), get()) }
        addFactory { GetLanguagesWithAnimeSources(get(), get()) }
        addFactory { GetRemoteAnime(get()) }
        addFactory { GetAnimeSourcesWithFavoriteCount(get(), get()) }
        addFactory { GetAnimeSourcesWithNonLibraryAnime(get()) }
        addFactory { ToggleAnimeSource(get()) }
        addFactory { ToggleAnimeSourcePin(get()) }
        addFactory { SetMigrateSorting(get()) }
        addFactory { ToggleLanguage(get()) }

        // AM (CU) -->
        addFactory { GetAllAnime(get()) }
        addSingletonFactory<CustomAnimeRepository> { CustomAnimeRepositoryImpl(get<Application>()) }
        addFactory { GetCustomAnimeInfo(get()) }
        addFactory { SetCustomAnimeInfo(get()) }
        // <-- AM (CU)
    }
}
