package se.gustavkarlsson.chefgpt

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiAction
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.chats.v1.CHATS_V1_PATH
import se.gustavkarlsson.chefgpt.api.common.JoinId
import se.gustavkarlsson.chefgpt.api.files.v1.ApiUploadedFile
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

private val FAKE_CHAT_ID = JoinId.parse("11111111-1111-1111-1111-111111111111")
private val FAKE_JOIN_ID = JoinId.parse("22222222-2222-2222-2222-222222222222")

@ExtendWith(SnapshotExtension::class)
class ActionsSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun unauthenticated() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("$CHATS_V1_PATH/$FAKE_CHAT_ID/actions")
        }

    @Test
    fun `invalid chat id`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("$CHATS_V1_PATH/not-a-uuid/actions") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody<ApiAction>(ApiUserJoinedChat(FAKE_JOIN_ID))
            }
        }

    @Test
    fun `chat not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("$CHATS_V1_PATH/$FAKE_CHAT_ID/actions") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody<ApiAction>(ApiUserJoinedChat(FAKE_JOIN_ID))
            }
        }

    @Test
    fun `user joined chat`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val chat = createChat(sessionId)

            client.post("$CHATS_V1_PATH/${chat.id}/actions") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody<ApiAction>(ApiUserJoinedChat(FAKE_JOIN_ID))
            }
        }

    @Test
    fun `user sends message with attachments`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val chat = createChat(sessionId)

            client.post("$CHATS_V1_PATH/${chat.id}/actions") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody<ApiAction>(
                    ApiUserSendsMessage(
                        text = "Can you save this?",
                        attachments =
                            listOf(
                                ApiUploadedFile(
                                    url = "https://res.cloudinary.com/demo/image/upload/v1/page.jpg",
                                    mimeType = "image/jpeg",
                                    fileName = "page.jpg",
                                ),
                                ApiUploadedFile(
                                    url = "https://res.cloudinary.com/demo/raw/upload/v1/recipe.txt",
                                    mimeType = "text/plain",
                                    fileName = "recipe.txt",
                                ),
                            ),
                    ),
                )
            }
        }
}
