package se.gustavkarlsson.chefgpt.auth

class UserRegistrationRule(
    val validate: (name: String, password: String) -> RegistrationError?,
) {
    companion object {
        fun name(
            errorMessage: String,
            validate: (name: String) -> Boolean,
        ) = UserRegistrationRule { name, _ ->
            if (validate(name)) null else RegistrationError.InvalidUserName(errorMessage)
        }

        fun password(
            errorMessage: String,
            validate: (password: String) -> Boolean,
        ) = UserRegistrationRule { _, password ->
            if (validate(password)) null else RegistrationError.InvalidPassword(errorMessage)
        }
    }
}
