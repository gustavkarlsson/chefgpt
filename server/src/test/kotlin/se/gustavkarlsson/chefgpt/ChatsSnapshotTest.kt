package se.gustavkarlsson.chefgpt

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class ChatsSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/chats")
        }

    @Test
    fun `invalid session`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/chats") {
                header("Session-Id", "invalid-session-id")
            }
        }

    @Test
    fun `successful chat creation`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/chats") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `list chats empty`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.get("/chats") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `list chats with one chat`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            createChat(sessionId)

            client.get("/chats") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete chat`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val chat = createChat(sessionId)

            client.delete("/chats/${chat.id}") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete chat not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("/chats/11111111-1111-1111-1111-111111111111") {
                header("Session-Id", sessionId)
            }
        }
}
