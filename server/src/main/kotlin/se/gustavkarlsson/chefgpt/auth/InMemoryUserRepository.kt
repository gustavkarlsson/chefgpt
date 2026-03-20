package se.gustavkarlsson.chefgpt.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryUserRepository : UserRepository {
    private val users = mutableMapOf<Pair<String, String>, User>()
    private val mutex = Mutex()

    override suspend fun register(
        name: String,
        password: String,
    ): User? =
        mutex.withLock {
            val key = name to password
            if (key !in users) {
                val user = User(UserId.random())
                users[key] = user
                user
            } else {
                null
            }
        }

    override suspend fun login(
        name: String,
        password: String,
    ): User? =
        mutex.withLock {
            val key = name to password
            users[key]
        }
}
