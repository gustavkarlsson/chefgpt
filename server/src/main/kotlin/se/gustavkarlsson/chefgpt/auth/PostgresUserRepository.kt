package se.gustavkarlsson.chefgpt.auth

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.postgres.PostgresDatabasePool
import se.gustavkarlsson.chefgpt.postgres.useSingletonScope
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.uuid.toKotlinUuid

private const val SALT_BYTE_COUNT = 16

class PostgresUserRepository(
    private val dbPool: PostgresDatabasePool,
    private val rules: List<RegistrationRule> = emptyList(),
) : UserRepository {
    private val md5 = MessageDigest.getInstance("MD5")
    private val secureRandom = SecureRandom()

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
        val salt = generateSalt()
        val id =
            dbPool.useSingletonScope {
                userQueries
                    .insert(
                        username = name,
                        password_md5_hash = Base64.encode(hash(password, salt)),
                        password_salt = Base64.encode(salt),
                    ).executeAsOneOrNull()
            }
        return if (id != null) {
            Ok(User(UserId(id.toKotlinUuid()), name))
        } else {
            Err(RegistrationError.UsernameTaken)
        }
    }

    override suspend fun login(
        name: String,
        password: String,
    ): Result<User, LoginError> {
        val userRow =
            dbPool.useSingletonScope {
                userQueries
                    .selectByUsername(name)
                    .executeAsOneOrNull()
            } ?: return Err(LoginError.WrongCredentials)
        val salt = Base64.decode(userRow.password_salt)
        val expectedHash = Base64.encode(hash(password, salt))
        return if (userRow.password_md5_hash == expectedHash) {
            Ok(User(UserId(userRow.id.toKotlinUuid()), userRow.username))
        } else {
            Err(LoginError.WrongCredentials)
        }
    }

    override suspend operator fun contains(name: String): Boolean =
        dbPool.useSingletonScope {
            userQueries.existsByUsername(name).executeAsOne()
        }

    private fun generateSalt(): ByteArray = ByteArray(SALT_BYTE_COUNT).also { secureRandom.nextBytes(it) }

    private fun hash(
        password: String,
        salt: ByteArray,
    ): ByteArray = md5.digest(password.encodeToByteArray() + salt)
}
