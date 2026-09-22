package se.gustavkarlsson.chefgpt.jobs

import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.ApiError

sealed interface AwaitJobError {
    data class RequestFailed(
        val error: ClientError,
    ) : AwaitJobError

    data class JobFailed(
        val error: ApiError?,
    ) : AwaitJobError
}
