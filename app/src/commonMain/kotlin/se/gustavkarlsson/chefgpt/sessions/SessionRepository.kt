package se.gustavkarlsson.chefgpt.sessions

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.ErrorResponse

data class UserCredentials(
    val userName: UserName,
    val password: Password,
) {
    companion object {
        operator fun invoke(
            userName: String,
            password: String,
        ): UserCredentials = UserCredentials(UserName(userName), Password(password))
    }
}

interface SessionRepository {
    suspend fun getCurrentSession(): SessionCredentials?

    suspend fun clearCurrentSession(): Boolean

    suspend fun register(credentials: UserCredentials): Result<SessionCredentials, ErrorResponse>

    suspend fun login(credentials: UserCredentials): Result<SessionCredentials, ErrorResponse>
}
