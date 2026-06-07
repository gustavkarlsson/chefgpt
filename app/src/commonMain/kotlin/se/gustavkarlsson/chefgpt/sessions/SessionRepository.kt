package se.gustavkarlsson.chefgpt.sessions

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ClientError

sealed interface RegisterError {
    data class ServerError(
        val error: ClientError,
    ) : RegisterError

    data object StorageFailed : RegisterError
}

interface SessionRepository {
    suspend fun getCurrentSession(): Result<SessionCredentials?, Unit>

    suspend fun register(credentials: UserCredentials): Result<SessionCredentials, RegisterError>

    suspend fun login(credentials: UserCredentials): Result<SessionCredentials, ClientError>

    suspend fun logOut(): Boolean
}
