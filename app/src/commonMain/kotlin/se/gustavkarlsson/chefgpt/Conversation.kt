package se.gustavkarlsson.chefgpt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.UserAction

interface Conversation {
    val acceptingInput: StateFlow<Boolean>
    val actionHistory: Flow<ApiAction>

    suspend fun sendToAgent(action: UserAction)
}
