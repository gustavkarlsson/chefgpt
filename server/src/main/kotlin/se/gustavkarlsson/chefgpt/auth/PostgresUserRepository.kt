package se.gustavkarlsson.chefgpt.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import kotlin.uuid.toKotlinUuid

private const val BCRYPT_COST = 12

class PostgresUserRepository(
    private val db: DatabaseAccess,
    private val rules: List<RegistrationRule> = emptyList(),
) : UserRepository {
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
        val hash = hasher.hashToString(BCRYPT_COST, password.toCharArray())
        val id =
            db.use {
                userQueries
                    .insert(
                        username = name,
                        password_hash = hash,
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
            db.use {
                userQueries
                    .selectByUsername(name)
                    .executeAsOneOrNull()
            } ?: return Err(LoginError.WrongCredentials)
        val result = hashVerifier.verify(password.toCharArray(), userRow.password_hash)
        return if (result.verified) {
            Ok(User(UserId(userRow.id.toKotlinUuid()), userRow.username))
        } else {
            Err(LoginError.WrongCredentials)
        }
    }

    override suspend operator fun contains(name: String): Boolean =
        db.use {
            userQueries.existsByUsername(name).executeAsOne()
        }
}
