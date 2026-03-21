package se.gustavkarlsson.chefgpt.auth

import io.ktor.server.plugins.BadRequestException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// FIXME prevent hammering
class InMemoryUserRepository(
    private val rules: List<UserRegistrationRule> = emptyList(),
) : UserRepository {
    private val users = mutableMapOf<Pair<String, String>, User>()
    private val mutex = Mutex()

    override suspend fun register(
        name: String,
        password: String,
    ): User? =
        mutex.withLock {
            val registrationError =
                rules.firstNotNullOfOrNull { rule ->
                    rule.errorMessage.takeUnless { rule.validate(name, password) }
                }
            if (registrationError != null) {
                throw BadRequestException(registrationError) // FIXME Don't trow. Return a result instead
            }
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
