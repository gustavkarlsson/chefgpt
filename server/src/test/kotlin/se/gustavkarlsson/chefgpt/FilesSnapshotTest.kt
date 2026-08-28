package se.gustavkarlsson.chefgpt

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.FILE_NAME_HEADER
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class FilesSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun unauthenticated() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/files") {
                contentType(ContentType.Image.JPEG)
                setBody(byteArrayOf())
            }
        }

    @Test
    fun `upload image`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/files") {
                header("Session-Id", sessionId)
                header(FILE_NAME_HEADER, "dish.jpg")
                contentType(ContentType.Image.JPEG)
                setBody(byteArrayOf(1, 2, 3))
            }
        }

    @Test
    fun `upload pdf`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/files") {
                header("Session-Id", sessionId)
                header(FILE_NAME_HEADER, "recipe.pdf")
                contentType(ContentType.Application.Pdf)
                setBody(byteArrayOf(1, 2, 3))
            }
        }

    @Test
    fun `upload text`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/files") {
                header("Session-Id", sessionId)
                header(FILE_NAME_HEADER, "recipe.txt")
                contentType(ContentType.Text.Plain)
                setBody("Boil water")
            }
        }

    @Test
    fun `upload unsupported type`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/files") {
                header("Session-Id", sessionId)
                header(FILE_NAME_HEADER, "recipe.zip")
                contentType(ContentType.Application.Zip)
                setBody(byteArrayOf(1, 2, 3))
            }
        }
}
