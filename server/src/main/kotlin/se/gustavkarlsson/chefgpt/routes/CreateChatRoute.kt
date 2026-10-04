package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.chats.v1.CHATS_V1_PATH
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.toApi
import se.gustavkarlsson.chefgpt.requireSession

fun Route.createChatRoute() {
    val chatRepository = get<ChatRepository>()
    post(CHATS_V1_PATH) {
        val userId = call.requireSession().user.id
        val chat = chatRepository.create(userId)
        call.respond(HttpStatusCode.Created, chat.toApi())
    }
}
