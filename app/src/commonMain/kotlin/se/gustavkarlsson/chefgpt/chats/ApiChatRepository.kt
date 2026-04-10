package se.gustavkarlsson.chefgpt.chats

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ErrorResponse
import se.gustavkarlsson.chefgpt.api.ApiChat
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.sessions.SessionId

private val log = Logger.withTag("${ApiChatRepository::class.simpleName}")

class ApiChatRepository(
    private val client: ChefGptClient,
) : ChatRepository {
    override suspend fun create(sessionId: SessionId): Result<Chat, ErrorResponse> =
        client
            .createChat(sessionId)
            .onOk {
                log.i { "Created chat: ${it.id}" }
            }.onErr {
                log.e { "Failed to create chat" }
            }.map { it.toChat() }

    override suspend fun stream(sessionId: SessionId): Flow<List<Chat>> =
        client
            .listenToChats(sessionId)
            .map { chats ->
                chats.map { it.toChat() }
            }

    override suspend fun delete(
        sessionId: SessionId,
        chatId: ChatId,
    ): Result<Unit, ErrorResponse> =
        client
            .deleteChat(sessionId, chatId)
            .onOk {
                log.i { "Deleted chat: $chatId" }
            }.onErr {
                log.e { "Failed to delete chat: $chatId" }
            }
}

private fun ApiChat.toChat(): Chat = Chat(id, createdAt)
