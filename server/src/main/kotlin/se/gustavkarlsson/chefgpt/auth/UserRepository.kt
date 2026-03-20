package se.gustavkarlsson.chefgpt.auth

import se.gustavkarlsson.chefgpt.auth.UserId

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
