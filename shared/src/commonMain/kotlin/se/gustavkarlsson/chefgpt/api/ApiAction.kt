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
    val attachments: List<ApiAttachment> = emptyList(),
) : ApiAction {
    init {
        require(text == null || text.isNotBlank()) {
            "Text must not be blank"
        }
        require(text != null || attachments.isNotEmpty()) {
            "Message must contain text or attachments"
        }
    }
}

@Serializable
@SerialName("api-user-joined-chat")
data class ApiUserJoinedChat(
    val joinId: JoinId,
) : ApiAction
