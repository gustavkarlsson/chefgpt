package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.SharedFlow
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.Event

sealed interface Chat {
    val id: ChatId

    suspend fun append(event: Event)

    // Suspending because we might want to load the events first
    suspend fun events(): SharedFlow<Event>
}
