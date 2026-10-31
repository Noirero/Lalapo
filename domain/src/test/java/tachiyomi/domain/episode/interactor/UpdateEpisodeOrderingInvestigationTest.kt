package tachiyomi.domain.episode.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.episode.repository.EpisodeRepository

/**
 * Investigation probe: executes the real UpdateEpisode interactor, with only its
 * repository dependency controlled. It does NOT execute PlayerViewModel or SQLDelight.
 * Do not interpret this test alone as proof of a player-level stale-write bug.
 */
class UpdateEpisodeOrderingInvestigationTest {
    @Test
    fun olderRepositoryWriteCanCompleteAfterNewerWrite() = runTest {
        val repository = mockk<EpisodeRepository>()
        val releaseOldWrite = CompletableDeferred<Unit>()
        val oldWriteEntered = CompletableDeferred<Unit>()
        var storedProgress: Long? = null

        coEvery { repository.update(any()) } coAnswers {
            val update = firstArg<EpisodeUpdate>()
            if (update.lastSecondSeen == 1_000L) {
                oldWriteEntered.complete(Unit)
                releaseOldWrite.await()
            }
            storedProgress = update.lastSecondSeen
        }

        val interactor = UpdateEpisode(repository)
        val oldRequest = launch { interactor.await(EpisodeUpdate(id = 42L, lastSecondSeen = 1_000L)) }
        runCurrent()
        oldWriteEntered.isCompleted shouldBe true

        val newRequest = launch { interactor.await(EpisodeUpdate(id = 42L, lastSecondSeen = 2_000L)) }
        runCurrent()
        storedProgress shouldBe 2_000L

        releaseOldWrite.complete(Unit)
        runCurrent()
        oldRequest.join()
        newRequest.join()
        storedProgress shouldBe 1_000L
    }
}
