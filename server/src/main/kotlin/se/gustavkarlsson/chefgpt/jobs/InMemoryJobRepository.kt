package se.gustavkarlsson.chefgpt.jobs

import kotlinx.serialization.json.JsonElement
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiJob
import se.gustavkarlsson.chefgpt.api.ApiJobState
import se.gustavkarlsson.chefgpt.api.JobId
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

private val JOB_EXPIRY = 60.minutes

class InMemoryJobRepository(
    private val clock: Clock = Clock.System,
) : JobRepository {
    private val jobs = ConcurrentHashMap<JobId, ApiJob<JsonElement>>()

    override suspend fun create(): ApiJob<JsonElement> {
        expireOldJobs()
        val job =
            ApiJob<JsonElement>(
                id = JobId.random(),
                state = ApiJobState.Working,
                createdAt = clock.now(),
                finishedAt = null,
                result = null,
                error = null,
            )
        jobs[job.id] = job
        return job
    }

    override suspend operator fun get(jobId: JobId): ApiJob<JsonElement>? = jobs[jobId]

    override suspend fun succeed(
        jobId: JobId,
        result: JsonElement?,
    ) {
        jobs[jobId]?.let { job ->
            jobs[jobId] =
                job.copy(
                    state = ApiJobState.Success,
                    finishedAt = clock.now(),
                    result = result,
                    error = null,
                )
        }
    }

    override suspend fun fail(
        jobId: JobId,
        error: ApiError,
    ) {
        jobs[jobId]?.let { job ->
            jobs[jobId] =
                job.copy(
                    state = ApiJobState.Failure,
                    finishedAt = clock.now(),
                    result = null,
                    error = error,
                )
        }
    }

    private fun expireOldJobs() {
        val cutoff = clock.now() - JOB_EXPIRY
        jobs.values.removeIf { it.createdAt < cutoff }
    }
}
