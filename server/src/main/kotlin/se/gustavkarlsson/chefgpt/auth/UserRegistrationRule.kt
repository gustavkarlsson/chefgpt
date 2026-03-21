package se.gustavkarlsson.chefgpt.auth

class UserRegistrationRule(
    val errorMessage: String,
    val validate: (name: String, password: String) -> Boolean,
) {
    companion object {
        fun name(
            errorMessage: String,
            validate: (name: String) -> Boolean,
        ) = UserRegistrationRule(errorMessage) { name, _ -> validate(name) }

        fun password(
            errorMessage: String,
            validate: (password: String) -> Boolean,
        ) = UserRegistrationRule(errorMessage) { _, password -> validate(password) }
    }
}
