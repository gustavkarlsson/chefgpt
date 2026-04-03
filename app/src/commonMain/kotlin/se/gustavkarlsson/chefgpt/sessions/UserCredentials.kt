package se.gustavkarlsson.chefgpt.sessions

data class UserCredentials(
    val userName: UserName,
    val password: Password,
) {
    companion object {
        operator fun invoke(
            userName: String,
            password: String,
        ): UserCredentials = UserCredentials(UserName(userName), Password(password))
    }
}
