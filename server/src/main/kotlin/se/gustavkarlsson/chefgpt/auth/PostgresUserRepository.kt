package se.gustavkarlsson.chefgpt.auth

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import se.gustavkarlsson.chefgpt.db.withTransaction
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.uuid.Uuid

private const val SALT_BYTE_COUNT = 16

private object UserTable : UuidTable("user") {
    val username = text("username")
    val passwordHash = binary("password_md5_hash")
    val passwordSalt = binary("password_salt")
}

class UserDao(
    id: EntityID<Uuid>,
) : UuidEntity(id) {
    companion object : UuidEntityClass<UserDao>(UserTable)

    var username by UserTable.username
    var passwordHash by UserTable.passwordHash
    var passwordSalt by UserTable.passwordSalt
}

// TODO prevent hammering
class PostgresUserRepository(
    private val db: Database,
    private val rules: List<UserRegistrationRule> = emptyList(),
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

            val exists = UserDao.find { UserTable.username eq name }.limit(1).any()
            if (!exists) {
                val salt = generateSalt()
                val userDao =
                    UserDao.new {
                        username = name
                        passwordSalt = salt
                        passwordHash = hash(password, salt)
                    }
                Ok(userDao.toUser())
            } else {
                Err(RegistrationError.UsernameTaken)
            }
        }

    override suspend fun login(
        name: String,
        password: String,
    ): Result<User, LoginError> =
        db.withTransaction {
            val userDao =
                UserDao.find { UserTable.username eq name }.limit(1).firstOrNull() ?: return@withTransaction Err(
                    LoginError.WrongCredentials,
                )
            val expectedHash = hash(password, userDao.passwordSalt)
            if (userDao.passwordHash.contentEquals(expectedHash)) {
                Ok(userDao.toUser())
            } else {
                Err(LoginError.WrongCredentials)
            }
        }

    override suspend operator fun contains(name: String): Boolean =
        db.withTransaction {
            val userDao =
                UserDao.find { UserTable.username eq name }.limit(1).firstOrNull()
            userDao != null
        }

    private fun generateSalt(): ByteArray = ByteArray(SALT_BYTE_COUNT).also { secureRandom.nextBytes(it) }

    private fun hash(
        password: String,
        salt: ByteArray,
    ): ByteArray = md5.digest(password.encodeToByteArray() + salt)
}

private fun UserDao.toUser(): User = User(id = UserId(id.value), name = username)
