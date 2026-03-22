package se.gustavkarlsson.chefgpt

import se.gustavkarlsson.chefgpt.api.ImageUrl

sealed interface Message {
    val text: String?
    val imageUrl: ImageUrl?
}

sealed interface AiMessage : Message {
    data class Regular(
        override val text: String?,
    ) : AiMessage {
        override val imageUrl: Nothing?
            get() = null
    }

    data class Reasoning(
        override val text: String?,
    ) : AiMessage {
        override val imageUrl: Nothing?
            get() = null
    }
}

data class UserMessage(
    override val text: String?,
    override val imageUrl: ImageUrl? = null,
) : Message
