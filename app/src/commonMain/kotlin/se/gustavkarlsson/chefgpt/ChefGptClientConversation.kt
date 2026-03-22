package se.gustavkarlsson.chefgpt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.api.AgentMessage
import se.gustavkarlsson.chefgpt.api.AgentReasoning
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.End
import se.gustavkarlsson.chefgpt.api.JoinedChat
import se.gustavkarlsson.chefgpt.api.ToolCall
import se.gustavkarlsson.chefgpt.api.UserEvent
import se.gustavkarlsson.chefgpt.api.UserMessage
import kotlin.uuid.Uuid

fun CoroutineScope.joinConversation(
    client: ChefGptClient,
    chatId: ChatId,
): Conversation = ChefGptClientConversation(this, client, chatId)

private class ChefGptClientConversation(
    scope: CoroutineScope,
    private val client: ChefGptClient,
    private val chatId: ChatId,
) : Conversation {
    private val conversationState = MutableStateFlow(ConversationState())
    private val joinEvent = JoinedChat(Uuid.random()) // TODO Add random constructor on JoinedChat

    override val acceptingInput: StateFlow<Boolean> =
        conversationState
            .map { state ->
                state.isAcceptingInput
            }.stateIn(scope, SharingStarted.Eagerly, conversationState.value.isAcceptingInput)

    private val ConversationState.isAcceptingInput: Boolean
        get() = isCaughtUp && isUsersTurn

    // TODO Return only events that are relevant to the conversation
    override val messageHistory: Flow<Message> =
        client
            .listenToEvents(chatId)
            .buffer(100)
            .onEach { event ->
                conversationState.update { state ->
                    val newIsUsersTurn =
                        when (event) {
                            is AgentMessage -> true
                            is AgentReasoning -> false
                            End -> false
                            ToolCall -> false
                            is UserMessage -> false
                            is JoinedChat -> null
                        }
                    val newIsCaughtUp =
                        if (state.isCaughtUp) {
                            true
                        } else {
                            joinEvent == event
                        }
                    state.copy(isUsersTurn = newIsUsersTurn ?: state.isUsersTurn, isCaughtUp = newIsCaughtUp)
                }
            }.mapNotNull { event ->
                when (event) {
                    is AgentMessage -> {
                        AiMessage.Regular(event.text)
                    }

                    is AgentReasoning -> {
                        AiMessage.Reasoning(event.text)
                    }

                    End -> {
                        null
                    }

                    ToolCall -> {
                        null
                    }

                    // TODO Map to tool call message
                    is UserMessage -> {
                        se.gustavkarlsson.chefgpt.UserMessage(
                            event.text,
                            event.imageUrl,
                        )
                    }

                    // TODO Rename so we can avoid FQ name
                    is JoinedChat -> {
                        null
                    }
                }
            }.shareIn(scope, SharingStarted.Eagerly, replay = Int.MAX_VALUE)

    init {
        scope.launch {
            // TODO Add some kind of retry loop
            client.sendEvent(chatId, joinEvent)
        }
        scope.coroutineContext.job.invokeOnCompletion {
            conversationState.update { it.copy(isUsersTurn = false) }
        }
    }

    override suspend fun sendToAgent(event: UserEvent) {
        conversationState.update { it.copy(isUsersTurn = false) }
        client.sendEvent(chatId, event)
    }
}

private data class ConversationState(
    val isUsersTurn: Boolean = true,
    val isCaughtUp: Boolean = false,
)
