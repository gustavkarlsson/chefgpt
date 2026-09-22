package se.gustavkarlsson.chefgpt.sessions

import com.github.michaelbull.result.Result

fun interface Register {
    suspend operator fun invoke(credentials: UserCredentials): Result<SessionCredentials, RegisterError>
}

class HttpRegister(
    private val repository: SessionRepository,
) : Register {
    override suspend fun invoke(credentials: UserCredentials): Result<SessionCredentials, RegisterError> =
        repository.register(credentials)
}
