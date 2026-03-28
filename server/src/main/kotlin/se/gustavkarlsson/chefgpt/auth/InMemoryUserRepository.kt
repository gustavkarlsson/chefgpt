package se.gustavkarlsson.chefgpt.auth

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
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
    ): Result<User, RegistrationError> {
        val registrationError =
            rules.firstNotNullOfOrNull { rule ->
                rule.validate(name, password)
            }
        if (registrationError != null) {
            return Err(registrationError)
        }

        val user = User(id = UserId.random(), name = name)
        val alreadyRegistered = users.putIfAbsent(name, UserData(user, hash(password)))
        return if (alreadyRegistered == null) {
            Ok(user)
        } else {
            Err(RegistrationError.UsernameTaken)
        }
    }

    override suspend fun login(
        name: String,
        password: String,
    ): Result<User, LoginError> {
        val userData = users[name] ?: return Err(LoginError.WrongCredentials)
        return if (userData.passwordHash == hash(password)) {
            Ok(userData.user)
        } else {
            Err(LoginError.WrongCredentials)
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
