package se.gustavkarlsson.chefgpt.auth

import com.github.michaelbull.result.Result

interface UserRepository {
    suspend fun register(
        name: String,
        password: String,
    ): Result<User, RegistrationError>

    suspend fun login(
        name: String,
        password: String,
    ): Result<User, LoginError>

    suspend operator fun contains(name: String): Boolean
}

sealed interface RegistrationError {
    data class InvalidUserName(
        val message: String,
    ) : RegistrationError

    data class InvalidPassword(
        val message: String,
    ) : RegistrationError

    data object UsernameTaken : RegistrationError
}

enum class LoginError {
    WrongCredentials,
}
