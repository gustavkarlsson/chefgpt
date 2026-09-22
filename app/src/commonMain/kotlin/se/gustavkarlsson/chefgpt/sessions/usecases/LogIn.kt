package se.gustavkarlsson.chefgpt.sessions.usecases

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.sessions.SessionCredentials
import se.gustavkarlsson.chefgpt.sessions.SessionRepository
import se.gustavkarlsson.chefgpt.sessions.UserCredentials

fun interface LogIn {
    suspend operator fun invoke(credentials: UserCredentials): Result<SessionCredentials, ClientError>
}

class HttpLogIn(
    private val repository: SessionRepository,
) : LogIn {
    override suspend fun invoke(credentials: UserCredentials): Result<SessionCredentials, ClientError> =
        repository.login(credentials)
}
