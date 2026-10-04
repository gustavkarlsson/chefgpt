package se.gustavkarlsson.chefgpt.jobs

import kotlinx.serialization.json.JsonElement
import se.gustavkarlsson.chefgpt.api.common.JobId
import se.gustavkarlsson.chefgpt.api.errors.v1.ApiError
import se.gustavkarlsson.chefgpt.api.jobs.v1.ApiJob

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
