package se.gustavkarlsson.chefgpt

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.session
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.header
import io.ktor.server.sse.SSE
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.Session
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.auth.registrationRules
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryChatRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository

fun Application.testModule() {
    install(ContentNegotiation) { json() }
    install(SSE)
    install(Sessions) {
        header<Session>("Session-Id", SessionStorageMemory())
    }
    install(Koin) {
        modules(
            module {
                single<UserRepository> { InMemoryUserRepository(registrationRules) }
                single<ChatRepository> { InMemoryChatRepository() }
                single<EventRepository> { InMemoryEventRepository() }
            },
        )
    }
    authentication {
        session<Session> {
            validate { session ->
                val userRepository: UserRepository by application.inject()
                session.takeIf { it.user.name in userRepository }
            }
            challenge { call.respond(HttpStatusCode.Unauthorized) }
        }
    }
    routing { routes() }
}
