package se.gustavkarlsson.chefgpt.sessions

data class UserCredentials(
    val userName: UserName,
    val password: Password,
)

interface SessionRepository {
    suspend fun getCurrentSession(): SessionCredentials?

    suspend fun register(userCredentials: UserCredentials): SessionCredentials

    suspend fun login(userCredentials: UserCredentials): SessionCredentials
}
