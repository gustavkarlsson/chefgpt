package se.gustavkarlsson.chefgpt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import se.gustavkarlsson.chefgpt.api.UserEvent

interface Conversation {
    val acceptingInput: StateFlow<Boolean>
    val messageHistory: Flow<Message>

    suspend fun sendToAgent(event: UserEvent)
}
