package se.gustavkarlsson.chefgpt.auth

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.uuid.toKotlinUuid

private const val SALT_BYTE_COUNT = 16

class PostgresUserRepository(
    private val db: PostgresAccess,
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
            db.use {
                userQueries
                    .insert(
                        username = name,
                        password_md5_hash = hash(password, salt),
                        password_salt = salt,
                    ).awaitAsOneOrNull()
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
            db.use {
                userQueries
                    .selectByUsername(name)
                    .awaitAsOneOrNull()
            } ?: return Err(LoginError.WrongCredentials)
        val expectedHash = hash(password, userRow.password_salt)
        return if (userRow.password_md5_hash.contentEquals(expectedHash)) {
            Ok(User(UserId(userRow.id.toKotlinUuid()), userRow.username))
        } else {
            Err(LoginError.WrongCredentials)
        }
    }

    override suspend operator fun contains(name: String): Boolean =
        db.use {
            userQueries.existsByUsername(name).awaitAsOne()
        }

    private fun generateSalt(): ByteArray = ByteArray(SALT_BYTE_COUNT).also { secureRandom.nextBytes(it) }

    private fun hash(
        password: String,
        salt: ByteArray,
    ): ByteArray = md5.digest(password.encodeToByteArray() + salt)
}
