package se.gustavkarlsson.chefgpt.jobs

import kotlinx.serialization.json.JsonElement
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiJob
import se.gustavkarlsson.chefgpt.api.JobId

interface JobRepository {
    suspend fun create(): ApiJob<JsonElement>

    suspend operator fun get(jobId: JobId): ApiJob<JsonElement>?

    suspend fun succeed(
        jobId: JobId,
        result: JsonElement?,
    )

    suspend fun fail(
        jobId: JobId,
        error: ApiError,
    )
}
