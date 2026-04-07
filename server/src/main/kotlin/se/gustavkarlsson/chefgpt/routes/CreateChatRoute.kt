package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.toApi

fun Route.createChatRoute() {
    post("/chats") {
        val chatRepository = get<ChatRepository>()
        val userId = call.requireSession().user.id
        val chat = chatRepository.create(userId)
        call.respond(HttpStatusCode.Created, chat.toApi())
    }
}
