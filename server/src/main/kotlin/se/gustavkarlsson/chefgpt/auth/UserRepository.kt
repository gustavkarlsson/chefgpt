package se.gustavkarlsson.chefgpt.auth

interface UserRepository {
    suspend fun register(
        name: String,
        password: String,
    ): User?

    suspend fun login(
        name: String,
        password: String,
    ): User?
}
