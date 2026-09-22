package se.gustavkarlsson.chefgpt.sessions

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError

fun interface LogIn {
    suspend operator fun invoke(credentials: UserCredentials): Result<SessionCredentials, ClientError>
}

class HttpLogIn(
    private val repository: SessionRepository,
) : LogIn {
    override suspend fun invoke(credentials: UserCredentials): Result<SessionCredentials, ClientError> =
        repository.login(credentials)
}
