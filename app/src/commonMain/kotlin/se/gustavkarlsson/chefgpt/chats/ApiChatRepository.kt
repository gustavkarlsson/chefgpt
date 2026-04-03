package se.gustavkarlsson.chefgpt.chats

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ErrorResponse
import se.gustavkarlsson.chefgpt.api.ApiChat
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.sessions.SessionId

class ApiChatRepository(
    private val client: ChefGptClient,
) : ChatRepository {
    private val refreshFlow =
        MutableSharedFlow<Unit>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            extraBufferCapacity = 1,
        ).apply {
            tryEmit(Unit)
        }

    override suspend fun create(sessionId: SessionId): Result<Chat, ErrorResponse> =
        client
            .createChat(sessionId)
            .map { apiChat ->
                refreshFlow.emit(Unit)
                apiChat.toChat()
            }

    // TODO Use a streaming endpoint
    override suspend fun getAll(sessionId: SessionId): Flow<Result<List<Chat>, ErrorResponse>> =
        refreshFlow
            .map {
                client
                    .getAllChats(sessionId)
                    .map { chats ->
                        chats.map { it.toChat() }
                    }
            }.distinctUntilChanged()

    override suspend fun delete(
        sessionId: SessionId,
        chatId: ChatId,
    ): Result<Unit, ErrorResponse> = client.deleteChat(sessionId, chatId)
}

private fun ApiChat.toChat(): Chat = Chat(id, createdAt)
