package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ChatId

interface Chat {
    val id: ChatId

    suspend fun send(apiEvent: ApiAction)

    fun streamEvents(): Flow<ApiEvent>
}
