package se.gustavkarlsson.chefgpt.jobs

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiJobState
import se.gustavkarlsson.chefgpt.api.JobId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private class MutableClock(
    private var now: Instant,
) : Clock {
    override fun now(): Instant = now

    fun advanceBy(duration: Duration) {
        now += duration
    }
}

class InMemoryJobRepositoryTest {
    private val clock = MutableClock(Instant.fromEpochSeconds(0))
    private val repo = InMemoryJobRepository(clock)

    @Test
    fun `create returns a working job`() =
        runTest {
            val job = repo.create()

            assertEquals(ApiJobState.Working, job.state)
            assertNull(job.finishedAt)
            assertNull(job.result)
            assertNull(job.error)
        }

    @Test
    fun `create assigns distinct ids to each job`() =
        runTest {
            val job1 = repo.create()
            val job2 = repo.create()

            assertNotEquals(job1.id, job2.id)
        }

    @Test
    fun `get returns the created job`() =
        runTest {
            val job = repo.create()

            assertEquals(job, repo.get(job.id))
        }

    @Test
    fun `get returns null for unknown job id`() =
        runTest {
            assertNull(repo.get(JobId.random()))
        }

    @Test
    fun `succeed sets success state and result`() =
        runTest {
            val job = repo.create()
            val result = JsonArray(listOf(JsonPrimitive("tomato")))

            repo.succeed(job.id, result)

            val updated = repo.get(job.id)!!
            assertEquals(ApiJobState.Success, updated.state)
            assertEquals(result, updated.result)
            assertNull(updated.error)
            assertEquals(clock.now(), updated.finishedAt)
        }

    @Test
    fun `fail sets failure state and error`() =
        runTest {
            val job = repo.create()
            val error = ApiError("agent-failed", "Boom", userMessage = null)

            repo.fail(job.id, error)

            val updated = repo.get(job.id)!!
            assertEquals(ApiJobState.Failure, updated.state)
            assertEquals(error, updated.error)
            assertNull(updated.result)
            assertEquals(clock.now(), updated.finishedAt)
        }

    @Test
    fun `create prunes jobs older than an hour`() =
        runTest {
            val oldJob = repo.create()
            clock.advanceBy(61.minutes)

            repo.create()

            assertNull(repo.get(oldJob.id))
        }

    @Test
    fun `create keeps jobs newer than an hour`() =
        runTest {
            val recentJob = repo.create()
            clock.advanceBy(59.minutes)

            repo.create()

            assertNotNull(repo.get(recentJob.id))
        }
}
