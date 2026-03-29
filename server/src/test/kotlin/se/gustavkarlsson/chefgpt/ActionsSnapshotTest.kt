package se.gustavkarlsson.chefgpt

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension
import kotlin.uuid.Uuid

private val FAKE_CHAT_ID = Uuid.parse("11111111-1111-1111-1111-111111111111")
private val FAKE_JOIN_ID = Uuid.parse("22222222-2222-2222-2222-222222222222")

@ExtendWith(SnapshotExtension::class)
class ActionsSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/chats/$FAKE_CHAT_ID/actions")
        }

    @Test
    fun `invalid chat id`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/chats/not-a-uuid/actions") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody<ApiAction>(ApiUserJoinedChat(FAKE_JOIN_ID))
            }
        }

    @Test
    fun `chat not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/chats/$FAKE_CHAT_ID/actions") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody<ApiAction>(ApiUserJoinedChat(FAKE_JOIN_ID))
            }
        }

    @Test
    fun `user joined chat`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val chatId = createChat(sessionId)

            client.post("/chats/$chatId/actions") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody<ApiAction>(ApiUserJoinedChat(FAKE_JOIN_ID))
            }
        }
}
