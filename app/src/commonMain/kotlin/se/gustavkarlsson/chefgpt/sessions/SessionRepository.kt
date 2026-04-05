package se.gustavkarlsson.chefgpt.sessions

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ErrorResponse

interface SessionRepository {
    suspend fun getCurrentSession(): Result<SessionCredentials?, Unit>

    suspend fun register(credentials: UserCredentials): Result<SessionCredentials, ErrorResponse>

    suspend fun login(credentials: UserCredentials): Result<SessionCredentials, ErrorResponse>

    suspend fun logOut(): Boolean
}
