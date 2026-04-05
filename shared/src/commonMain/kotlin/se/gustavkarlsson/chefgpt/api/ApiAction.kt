package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An action is something that the user has done to update the chat. It will often lead to an [ApiEvent].
 */
@Serializable
@SerialName("api-action")
sealed interface ApiAction

@Serializable
@SerialName("api-user-sends-message")
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
@SerialName("api-user-joined-chat")
data class ApiUserJoinedChat(
    val joinId: JoinId,
) : ApiAction
