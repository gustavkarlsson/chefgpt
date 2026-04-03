package se.gustavkarlsson.chefgpt.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.sessions.SessionId

@Serializable
@SerialName("route")
sealed interface Route : NavKey {
    @Serializable
    @SerialName("start")
    data object Start : Route

    @Serializable
    @SerialName("chat")
    data class Chat(
        val sessionId: SessionId,
        val chatId: ChatId,
    ) : Route
}
