package se.gustavkarlsson.chefgpt.sessions.usecases

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.sessions.RegisterError
import se.gustavkarlsson.chefgpt.sessions.SessionCredentials
import se.gustavkarlsson.chefgpt.sessions.SessionRepository
import se.gustavkarlsson.chefgpt.sessions.UserCredentials

fun interface Register {
    suspend operator fun invoke(credentials: UserCredentials): Result<SessionCredentials, RegisterError>
}

class HttpRegister(
    private val repository: SessionRepository,
) : Register {
    override suspend fun invoke(credentials: UserCredentials): Result<SessionCredentials, RegisterError> =
        repository.register(credentials)
}
