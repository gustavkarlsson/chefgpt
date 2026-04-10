package se.gustavkarlsson.chefgpt.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.util.concurrent.ConcurrentHashMap

private const val BCRYPT_COST = 12

// TODO prevent hammering
class InMemoryUserRepository(
    private val rules: List<RegistrationRule> = emptyList(),
) : UserRepository {
    private val users = ConcurrentHashMap<String, UserData>()
    private val hasher = BCrypt.withDefaults()
    private val hashVerifier = BCrypt.verifyer()

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
        val hash = hasher.hashToString(BCRYPT_COST, password.toCharArray())
        val alreadyRegistered = users.putIfAbsent(name, UserData(user, hash))
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
        return if (hashVerifier.verify(password.toCharArray(), userData.passwordHash).verified) {
            Ok(userData.user)
        } else {
            Err(LoginError.WrongCredentials)
        }
    }

    override suspend operator fun contains(name: String): Boolean = users.containsKey(name)
}

private data class UserData(
    val user: User,
    val passwordHash: String,
)
