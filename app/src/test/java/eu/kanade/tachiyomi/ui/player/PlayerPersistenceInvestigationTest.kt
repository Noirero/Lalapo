package eu.kanade.tachiyomi.ui.player

import eu.kanade.tachiyomi.data.database.models.Episode
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.episode.interactor.UpdateEpisode
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.episode.repository.EpisodeRepository
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.repository.HistoryRepository
import java.util.Date

/**
 * Investigation-only. Executes the exact production persistence sequencing helper,
 * and production interactors where noted. Does not instantiate PlayerViewModel or SQLDelight.
 */
class PlayerPersistenceInvestigationTest {
    private fun episode(id: Long): Episode = mockk<Episode> {
        every { this@mockk.id } returns id
    }

    @Test
    fun olderSaveCanFinishLastAtControlledRepositoryBoundary() = runTest {
        val ep = episode(42L)
        val repository = mockk<EpisodeRepository>()
        val releaseOld = CompletableDeferred<Unit>()
        val enteredOld = CompletableDeferred<Unit>()
        var persisted: Long? = null
        coEvery { repository.update(any()) } coAnswers {
            val payload = firstArg<EpisodeUpdate>()
            if (payload.lastSecondSeen == 1000L) {
                enteredOld.complete(Unit)
                releaseOld.await()
            }
            persisted = payload.lastSecondSeen
        }
        val interactor = UpdateEpisode(repository)
        val old = launch {
            persistWatchingProgress(ep, { interactor.await(EpisodeUpdate(id = 42L, lastSecondSeen = 1000L)) }, {})
        }
        runCurrent()
        enteredOld.isCompleted shouldBe true
        val newer = launch {
            persistWatchingProgress(ep, { interactor.await(EpisodeUpdate(id = 42L, lastSecondSeen = 2000L)) }, {})
        }
        runCurrent()
        persisted shouldBe 2000L
        releaseOld.complete(Unit)
        runCurrent()
        old.join()
        newer.join()
        persisted shouldBe 1000L
    }

    @Test
    fun mutableEpisodeIsReadWhenSuspendedSaveResumes() = runTest {
        val ep = episode(42L)
        var progress = 1000L
        every { ep.last_second_seen } answers { progress }
        val gate = CompletableDeferred<Unit>()
        var persisted: Long? = null
        val request = launch {
            gate.await()
            persistWatchingProgress(ep, { persisted = it.last_second_seen }, {})
        }
        runCurrent()
        progress = 2000L
        gate.complete(Unit)
        runCurrent()
        request.join()
        persisted shouldBe 2000L
    }

    @Test
    fun swallowedEpisodeUpdateFailureAllowsHistoryUpsert() = runTest {
        val ep = episode(42L)
        val episodeRepository = mockk<EpisodeRepository>()
        val historyRepository = mockk<HistoryRepository>()
        coEvery { episodeRepository.update(any()) } throws IllegalStateException("injected episode failure")
        coEvery { historyRepository.upsertHistory(any()) } returns Unit
        val updateEpisode = UpdateEpisode(episodeRepository)
        val upsertHistory = UpsertHistory(historyRepository)
        persistWatchingProgress(
            ep,
            { updateEpisode.await(EpisodeUpdate(id = it.id!!, lastSecondSeen = 1000L)) },
            { upsertHistory.await(HistoryUpdate(it.id!!, Date(1000L))) },
        )
        coVerify(exactly = 1) { episodeRepository.update(any()) }
        coVerify(exactly = 1) { historyRepository.upsertHistory(any()) }
    }
}
