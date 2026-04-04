package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.toApi

fun Routing.createChatRoute() {
    authenticate {
        post("/chats") {
            val chatRepository = get<ChatRepository>()
            val session = call.requireSession()
            val chat = chatRepository.create(session.user.id)
            call.respond(HttpStatusCode.Created, chat.toApi())
        }
    }
}
