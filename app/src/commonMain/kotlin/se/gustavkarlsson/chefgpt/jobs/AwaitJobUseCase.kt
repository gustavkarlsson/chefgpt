package se.gustavkarlsson.chefgpt.jobs

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.mapError
import kotlinx.coroutines.delay
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiJob
import se.gustavkarlsson.chefgpt.api.ApiJobState
import se.gustavkarlsson.chefgpt.api.JobId
import se.gustavkarlsson.chefgpt.sessions.SessionId
import kotlin.time.Duration.Companion.seconds

private val POLL_INTERVAL = 1.seconds

class AwaitJobUseCase(
    private val client: ChefGptClient,
) {
    suspend fun await(
        sessionId: SessionId,
        createJob: suspend () -> Result<ApiJob, ClientError>,
    ): Result<ApiJob, AwaitJobError> =
        createJob()
            .mapError { AwaitJobError.RequestFailed(it) }
            .flatMap { job -> poll(sessionId, job.id) }

    private suspend fun poll(
        sessionId: SessionId,
        jobId: JobId,
    ): Result<ApiJob, AwaitJobError> {
        while (true) {
            delay(POLL_INTERVAL)
            val polled: Result<ApiJob, AwaitJobError>? =
                client.getJob(sessionId, jobId).fold(
                    success = { job ->
                        when (job.state) {
                            ApiJobState.Working -> null
                            ApiJobState.Success -> Ok(job)
                            ApiJobState.Failure -> Err(AwaitJobError.JobFailed(job.error))
                        }
                    },
                    failure = { error -> Err(AwaitJobError.RequestFailed(error)) },
                )
            if (polled != null) {
                return polled
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
