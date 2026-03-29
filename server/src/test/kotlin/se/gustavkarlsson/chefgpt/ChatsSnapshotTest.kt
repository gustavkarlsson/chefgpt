package se.gustavkarlsson.chefgpt

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
}
