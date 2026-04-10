package se.gustavkarlsson.chefgpt.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.send
import kotlinx.coroutines.flow.collectLatest
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.toApi
import se.gustavkarlsson.chefgpt.util.sse

// TODO Add tests (Not snapshot test, as they are not possible)
fun Route.streamChatsRoute() {
    sse("/chats") {
        val chatRepository = get<ChatRepository>()
        val userId = call.requireSession().user.id

        chatRepository
            .stream(userId)
            .collectLatest { chats ->
                send(chats.map { it.toApi() }, "chats")
            }
    }
}
