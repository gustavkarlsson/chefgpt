package se.gustavkarlsson.chefgpt

import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.chats.v1.CHATS_V1_PATH
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
            client.post(CHATS_V1_PATH)
        }

    @Test
    fun `invalid session`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post(CHATS_V1_PATH) {
                header("Session-Id", "invalid-session-id")
            }
        }

    @Test
    fun `successful chat creation`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post(CHATS_V1_PATH) {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete chat`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val chat = createChat(sessionId)

            client.delete("$CHATS_V1_PATH/${chat.id}") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete chat not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("$CHATS_V1_PATH/11111111-1111-1111-1111-111111111111") {
                header("Session-Id", sessionId)
            }
        }
}
