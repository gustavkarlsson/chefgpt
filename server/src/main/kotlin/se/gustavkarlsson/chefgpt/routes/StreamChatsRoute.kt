package se.gustavkarlsson.chefgpt.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.send
import io.ktor.server.sse.sse
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.toApi
import kotlin.time.Duration.Companion.seconds

// TODO Add tests (Not snapshot test, as they are not possible)
fun Route.streamChatsRoute() {
    sse(
        "/chats",
        serialize = { typeInfo, value ->
            val json = get<Json>()
            val serializer = json.serializersModule.serializer(typeInfo.kotlinType!!)
            json.encodeToString(serializer, value)
        },
    ) {
        heartbeat {
            period = 15.seconds
        }
        val chatRepository = get<ChatRepository>()
        val userId = call.requireSession().user.id

        chatRepository
            .stream(userId)
            .collectLatest { chats ->
                send(chats.map { it.toApi() }, "chats")
            }
    }
}
