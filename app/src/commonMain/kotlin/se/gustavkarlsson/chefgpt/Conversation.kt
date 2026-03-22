package se.gustavkarlsson.chefgpt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import se.gustavkarlsson.chefgpt.api.Action
import se.gustavkarlsson.chefgpt.api.UserAction

interface Conversation {
    val acceptingInput: StateFlow<Boolean>
    val actionHistory: Flow<Action>

    suspend fun sendToAgent(action: UserAction)
}
