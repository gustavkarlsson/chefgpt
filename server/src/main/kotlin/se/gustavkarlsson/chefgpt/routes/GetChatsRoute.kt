package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.toApi

fun Route.getChatsRoute() {
    get("/chats") {
        val chatRepository = get<ChatRepository>()
        val session = call.requireSession()
        val chats = chatRepository.getAll(session.user.id)
        call.respond(HttpStatusCode.OK, chats.map { it.toApi() })
    }
}
