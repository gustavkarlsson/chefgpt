package se.gustavkarlsson.chefgpt.auth

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import se.gustavkarlsson.chefgpt.db.withTransaction
import java.security.MessageDigest
import java.security.SecureRandom

private const val SALT_BYTE_COUNT = 16

private object UserTable : UuidTable("user") {
    val username = text("username")
    val passwordHash = binary("password_md5_hash")
    val passwordSalt = binary("password_salt")
}

// TODO prevent hammering
class PostgresUserRepository(
    private val db: R2dbcDatabase,
    private val rules: List<RegistrationRule> = emptyList(),
) : UserRepository {
    private val md5 = MessageDigest.getInstance("MD5")
    private val secureRandom = SecureRandom()

    override suspend fun register(
        name: String,
        password: String,
    ): Result<User, RegistrationError> =
        db.withTransaction {
            val registrationError =
                rules.firstNotNullOfOrNull { rule ->
                    rule.validate(name, password)
                }
            if (registrationError != null) {
                return@withTransaction Err(registrationError)
            }

            val exists =
                UserTable
                    .selectAll()
                    .where { UserTable.username eq name }
                    .limit(1)
                    .empty()
                    .not()
            if (!exists) {
                val salt = generateSalt()
                val newId = UserId.random()
                UserTable.insert {
                    it[id] = newId.value // TODO Let postgres generate the ID?
                    it[username] = name
                    it[passwordSalt] = salt
                    it[passwordHash] = hash(password, salt)
                }
                Ok(User(newId, name))
            } else {
                Err(RegistrationError.UsernameTaken)
            }
        }

    override suspend fun login(
        name: String,
        password: String,
    ): Result<User, LoginError> =
        db.withTransaction {
            // TODO Make more functional?
            val row =
                UserTable
                    .selectAll()
                    .where { UserTable.username eq name }
                    .limit(1)
                    .firstOrNull() ?: return@withTransaction Err(LoginError.WrongCredentials)
            val expectedHash = hash(password, row[UserTable.passwordSalt])
            if (row[UserTable.passwordHash].contentEquals(expectedHash)) {
                Ok(User(id = UserId(row[UserTable.id].value), name = row[UserTable.username]))
            } else {
                Err(LoginError.WrongCredentials)
            }
        }

    override suspend operator fun contains(name: String): Boolean =
        db.withTransaction {
            UserTable
                .selectAll()
                .where { UserTable.username eq name }
                .limit(1)
                .empty()
                .not()
        }

    private fun generateSalt(): ByteArray = ByteArray(SALT_BYTE_COUNT).also { secureRandom.nextBytes(it) }

    private fun hash(
        password: String,
        salt: ByteArray,
    ): ByteArray = md5.digest(password.encodeToByteArray() + salt)
}
