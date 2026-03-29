package se.gustavkarlsson.chefgpt

import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.session
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.header
import io.ktor.server.sse.SSE
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.auth.InMemoryUserRepository
import se.gustavkarlsson.chefgpt.auth.Session
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.auth.registrationRules
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension
import se.gustavkarlsson.slapshot.ktor3.SnapshotTesting

@ExtendWith(SnapshotExtension::class)
class RoutesSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `register without credentials`() =
        testApplication {
            application { testModule() }
            val client =
                createClient {
                    install(SnapshotTesting(snapshotContext))
                }

            client.post("/register")
        }
}

private fun Application.testModule() {
    install(ContentNegotiation) { json() }
    install(SSE)
    install(Sessions) {
        header<Session>("Session-Id", SessionStorageMemory())
    }
    authentication {
        session<Session> {
            validate { session ->
                val userRepository: UserRepository by application.dependencies
                session.takeIf { it.user.name in userRepository }
            }
            challenge { call.respond(HttpStatusCode.Unauthorized) }
        }
    }
    dependencies {
        provide<UserRepository> { InMemoryUserRepository(registrationRules) }
    }
    routing { routes() }
}
