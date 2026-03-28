package se.gustavkarlsson.chefgpt.auth

import io.ktor.server.plugins.BadRequestException
import kotlinx.io.bytestring.ByteString
import java.security.MessageDigest

// TODO prevent hammering
class InMemoryUserRepository(
    private val rules: List<UserRegistrationRule> = emptyList(),
) : UserRepository {
    private val users = mutableMapOf<String, UserData>()
    private val md5 = MessageDigest.getInstance("MD5")

    override suspend fun register(
        name: String,
        password: String,
    ): User? {
        val registrationError =
            rules.firstNotNullOfOrNull { rule ->
                rule.errorMessage.takeUnless { rule.validate(name, password) }
            }
        if (registrationError != null) {
            throw BadRequestException(registrationError) // TODO Don't throw. Return a result instead
        }

        val user = User(id = UserId.random(), name = name)
        val alreadyRegistered = users.putIfAbsent(name, UserData(user, hash(password)))
        return if (alreadyRegistered == null) {
            user
        } else {
            null
        }
    }

    override suspend fun login(
        name: String,
        password: String,
    ): User? {
        val userData = users[name] ?: return null
        return if (userData.passwordHash == hash(password)) {
            userData.user
        } else {
            null
        }
    }

    override suspend operator fun contains(name: String): Boolean = name in users

    // Not the safest way to do this, but it's fine for now
    private fun hash(password: String) = ByteString(md5.digest(password.encodeToByteArray()))
}

private data class UserData(
    val user: User,
    val passwordHash: ByteString,
)
