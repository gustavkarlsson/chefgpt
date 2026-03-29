package se.gustavkarlsson.chefgpt.auth

class RegistrationRule(
    val validate: (name: String, password: String) -> RegistrationError?,
) {
    companion object {
        fun name(
            errorMessage: String,
            validate: (name: String) -> Boolean,
        ) = RegistrationRule { name, _ ->
            if (validate(name)) null else RegistrationError.InvalidUserName(errorMessage)
        }

        fun password(
            errorMessage: String,
            validate: (password: String) -> Boolean,
        ) = RegistrationRule { _, password ->
            if (validate(password)) null else RegistrationError.InvalidPassword(errorMessage)
        }
    }
}
