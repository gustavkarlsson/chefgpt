package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * An action is something that the user has done to update the chat. It will often lead to an [ApiEvent].
 */
sealed interface ApiAction

@Serializable
data class ApiUserSendsMessage(
    val text: String?,
    val imageUrl: ImageUrl? = null,
) : ApiAction {
    init {
        require(text == null || text.isNotBlank()) {
            "Text must not be blank"
        }
        require(text != null || imageUrl != null) {
            "Message must contain text or imageUrl"
        }
    }
}

@Serializable
data class ApiUserJoinedChat(
    val joinId: Uuid,
) : ApiAction
