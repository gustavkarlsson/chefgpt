package se.gustavkarlsson.chefgpt.auth

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.toResultOr
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

// TODO Add index on username
private object Table : UuidTable("user") {
    val username = text("username")
    val passwordHash = binary("password_md5_hash")
    val passwordSalt = binary("password_salt")
}

// TODO Test with dev containers
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
    ): Result<User, RegistrationError> {
        val registrationError =
            rules.firstNotNullOfOrNull { rule ->
                rule.validate(name, password)
            }
        if (registrationError != null) {
            return Err(registrationError)
        }
        return db.withTransaction {
            val exists =
                Table
                    .selectAll()
                    .where { Table.username eq name }
                    .limit(1)
                    .empty()
                    .not()
            if (!exists) {
                val salt = generateSalt()
                val newId = UserId.random()
                Table.insert {
                    it[id] = newId.value // TODO Let postgres generate the ID?
                    it[username] = name
                    it[passwordHash] = hash(password, salt)
                    it[passwordSalt] = salt
                }
                Ok(User(newId, name))
            } else {
                Err(RegistrationError.UsernameTaken)
            }
        }
    }

    override suspend fun login(
        name: String,
        password: String,
    ): Result<User, LoginError> =
        db.withTransaction {
            Table
                .selectAll()
                .where { Table.username eq name }
                .limit(1)
                .firstOrNull()
                .toResultOr { LoginError.WrongCredentials }
                .flatMap { userRow ->
                    val id = userRow[Table.id]
                    val username = userRow[Table.username]
                    val salt = userRow[Table.passwordSalt]
                    val passwordHash = userRow[Table.passwordHash]
                    val expectedHash = hash(password, salt)
                    if (passwordHash.contentEquals(expectedHash)) {
                        Ok(User(UserId(id.value), username))
                    } else {
                        Err(LoginError.WrongCredentials)
                    }
                }
        }

    override suspend operator fun contains(name: String): Boolean =
        db.withTransaction {
            Table
                .selectAll()
                .where { Table.username eq name }
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
