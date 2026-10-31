package tachiyomi.domain.history.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.history.repository.HistoryRepository

class GetNextEpisodesTest {
    private val animeId = 10L
    private val getAnime = mockk<GetAnime>()
    private val getEpisodes = mockk<GetEpisodesByAnimeId>()
    private val subject = GetNextEpisodes(getEpisodes, getAnime, mockk<HistoryRepository>())

    private fun setup(episodes: List<Episode>) {
        val anime = mockk<Anime>()
        io.mockk.every { anime.sorting } returns Anime.EPISODE_SORTING_NUMBER
        coEvery { getAnime.await(animeId) } returns anime
        coEvery { getEpisodes.await(animeId, applyScanlatorFilter = true) } returns episodes
    }

    private fun episode(id: Long, seen: Boolean = false) = Episode.create().copy(
        id = id,
        animeId = animeId,
        sourceOrder = id,
        episodeNumber = id.toDouble(),
        seen = seen,
    )

    @Test
    fun `missing reference includes first available episode`() = runBlocking {
        val episodes = listOf(episode(1), episode(2), episode(3))
        setup(episodes)
        subject.await(animeId, 99, onlyUnseen = false) shouldBe episodes
    }

    @Test
    fun `missing reference with onlyUnseen returns unseen episodes`() = runBlocking {
        val episodes = listOf(episode(1, seen = true), episode(2), episode(3))
        setup(episodes)
        subject.await(animeId, 99, onlyUnseen = true) shouldBe episodes.drop(1)
    }

    @Test
    fun `scanlator filtered reference includes first visible episode`() = runBlocking {
        val visible = listOf(episode(2), episode(3))
        setup(visible)
        subject.await(animeId, 1, onlyUnseen = false) shouldBe visible
    }

    @Test
    fun `seen reference filtered by onlyUnseen starts at first unseen`() = runBlocking {
        val episodes = listOf(episode(1, seen = true), episode(2), episode(3))
        setup(episodes)
        subject.await(animeId, 1, onlyUnseen = true) shouldBe episodes.drop(1)
    }

    @Test
    fun `unseen reference remains first when including seen`() = runBlocking {
        val episodes = listOf(episode(1, seen = true), episode(2), episode(3))
        setup(episodes)
        subject.await(animeId, 2, onlyUnseen = false) shouldBe episodes.drop(1)
    }

    @Test
    fun `completed reference advances to next episode`() = runBlocking {
        val episodes = listOf(episode(1), episode(2, seen = true), episode(3))
        setup(episodes)
        subject.await(animeId, 2, onlyUnseen = false) shouldBe episodes.drop(2)
    }

    @Test
    fun `empty available list stays empty`() = runBlocking {
        setup(emptyList())
        subject.await(animeId, 99, onlyUnseen = false) shouldBe emptyList()
    }
}
