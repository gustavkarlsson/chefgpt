package se.gustavkarlsson.chefgpt

sealed interface MessageToUser {
    val text: String

    data class Regular(
        override val text: String,
    ) : MessageToUser

    data class Reasoning(
        override val text: String,
    ) : MessageToUser
}
