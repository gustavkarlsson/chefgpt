package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.util.getOrFail
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.requireSession

fun Route.deleteChatRoute() {
    delete("/chats/{chatId}") {
        val session = call.requireSession()
        val rawChatId = call.parameters.getOrFail("chatId")
        val chatId = ChatId.parseOrNull(rawChatId)
        if (chatId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("invalid-chat-id", "Invalid chat ID"),
            )
            return@delete
        }
        val chatRepository = get<ChatRepository>()
        val deleted = chatRepository.delete(session.user.id, chatId)
        if (deleted) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                ApiError("chat-not-found", "Chat not found"),
            )
        }
    }
}
