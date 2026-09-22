package se.gustavkarlsson.chefgpt.jobs

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiJob
import se.gustavkarlsson.chefgpt.api.ApiJobState
import se.gustavkarlsson.chefgpt.sessions.SessionId
import kotlin.time.Duration.Companion.seconds

private val POLL_INTERVAL = 1.seconds

// Not a `fun interface`: its single `invoke` is generic, which a SAM type cannot express.
interface AwaitJob {
    suspend operator fun <T> invoke(
        sessionId: SessionId,
        deserializer: KSerializer<T>,
        createJob: suspend () -> Result<ApiJob<T>, ClientError>,
    ): Result<ApiJob<T>, AwaitJobError>
}

class HttpAwaitJob(
    private val client: ChefGptClient,
    private val json: Json,
) : AwaitJob {
    override suspend fun <T> invoke(
        sessionId: SessionId,
        deserializer: KSerializer<T>,
        createJob: suspend () -> Result<ApiJob<T>, ClientError>,
    ): Result<ApiJob<T>, AwaitJobError> {
        val created = createJob()
        val job = created.get()
        if (job == null) {
            return Err(AwaitJobError.RequestFailed(created.getError()!!))
        }

        while (true) {
            delay(POLL_INTERVAL)
            val jobResult = client.getJob(sessionId, job.id)
            val polled = jobResult.get()
            if (polled == null) {
                return Err(AwaitJobError.RequestFailed(jobResult.getError()!!))
            }
            when (polled.state) {
                ApiJobState.Working -> {
                    Unit
                }

                ApiJobState.Success -> {
                    return Ok(
                        ApiJob(
                            id = polled.id,
                            state = polled.state,
                            createdAt = polled.createdAt,
                            finishedAt = polled.finishedAt,
                            result = polled.result?.let { json.decodeFromJsonElement(deserializer, it) },
                            error = polled.error,
                        ),
                    )
                }

                ApiJobState.Failure -> {
                    return Err(AwaitJobError.JobFailed(polled.error))
                }
            }
        }
    }
}

sealed interface AwaitJobError {
    data class RequestFailed(
        val error: ClientError,
    ) : AwaitJobError

    data class JobFailed(
        val error: ApiError?,
    ) : AwaitJobError
}
