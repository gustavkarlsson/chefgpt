package se.gustavkarlsson.chefgpt.auth

import io.ktor.server.plugins.BadRequestException
import kotlinx.io.bytestring.ByteString
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import se.gustavkarlsson.chefgpt.db.withTransaction
import java.security.MessageDigest
import kotlin.uuid.Uuid

private object UserTable : UuidTable("user") {
    val username = text("username")
    val passwordHash = binary("password_md5_hash", 16)
}

class UserDao(
    id: EntityID<Uuid>,
) : UuidEntity(id) {
    companion object : UuidEntityClass<UserDao>(UserTable)

    var username by UserTable.username
    var passwordHash by UserTable.passwordHash
}

// TODO prevent hammering
class PostgresUserRepository(
    private val db: Database,
    private val rules: List<UserRegistrationRule> = emptyList(),
) : UserRepository {
    private val md5 = MessageDigest.getInstance("MD5")

    override suspend fun register(
        name: String,
        password: String,
    ): User? =
        db.withTransaction {
            val registrationError =
                rules.firstNotNullOfOrNull { rule ->
                    rule.errorMessage.takeUnless { rule.validate(name, password) }
                }
            if (registrationError != null) {
                throw BadRequestException(registrationError) // TODO Don't throw. Return a result instead
            }

            val exists = UserDao.find { UserTable.username eq name }.limit(1).any()
            if (!exists) {
                val userDao =
                    UserDao.new {
                        username = name
                        passwordHash = hash(password).toByteArray()
                    }
                userDao.toUser()
            } else {
                null
            }
        }

    override suspend fun login(
        name: String,
        password: String,
    ): User? =
        db.withTransaction {
            UserDao
                .find {
                    (UserTable.username eq name) and (UserTable.passwordHash eq hash(password).toByteArray())
                }.limit(1)
                .firstOrNull()
                ?.toUser()
        }

    // Not the safest way to do this, but it's fine for now
    private fun hash(password: String) = ByteString(md5.digest(password.encodeToByteArray()))
}

private fun UserDao.toUser(): User = User(id = UserId(id.value), name = username)
