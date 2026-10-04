package se.gustavkarlsson.chefgpt

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.files.v1.ApiUploadedFile
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiScanRecipe
import se.gustavkarlsson.chefgpt.api.recipes.v1.RECIPES_V1_PATH
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
            client.post("$RECIPES_V1_PATH/scan") {
                contentType(ContentType.Application.Json)
                setBody(ApiScanRecipe(files = listOf(photo())))
            }
        }

    @Test
    fun `scan recipes`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("$RECIPES_V1_PATH/scan") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiScanRecipe(files = listOf(photo())))
            }
        }

    @Test
    fun `scan something that is not a photo`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("$RECIPES_V1_PATH/scan") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(
                    ApiScanRecipe(
                        files =
                            listOf(
                                ApiUploadedFile(
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
        ApiUploadedFile(
            url = "https://example.com/photo.jpg",
            mimeType = "image/jpeg",
            fileName = "photo.jpg",
        )
}
