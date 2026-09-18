package se.gustavkarlsson.chefgpt

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.api.ApiScanRecipe
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class ScanRecipesSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun unauthenticated() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/recipes/scan") {
                contentType(ContentType.Application.Json)
                setBody(ApiScanRecipe(attachments = listOf(photo())))
            }
        }

    @Test
    fun `scan recipes`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/recipes/scan") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiScanRecipe(attachments = listOf(photo())))
            }
        }

    @Test
    fun `scan something that is not a photo`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/recipes/scan") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(
                    ApiScanRecipe(
                        attachments =
                            listOf(
                                ApiAttachment(
                                    url = "https://example.com/recipe.pdf",
                                    mimeType = "application/pdf",
                                    fileName = "recipe.pdf",
                                ),
                            ),
                    ),
                )
            }
        }

    private fun photo() =
        ApiAttachment(
            url = "https://example.com/photo.jpg",
            mimeType = "image/jpeg",
            fileName = "photo.jpg",
        )
}
