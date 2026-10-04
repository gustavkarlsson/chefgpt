package se.gustavkarlsson.chefgpt

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiScrapeRecipe
import se.gustavkarlsson.chefgpt.api.recipes.v1.RECIPES_V1_PATH
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class ScrapeRecipesSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun unauthenticated() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("$RECIPES_V1_PATH/scrape") {
                contentType(ContentType.Application.Json)
                setBody(ApiScrapeRecipe(url = "https://example.com/recipe"))
            }
        }

    @Test
    fun `scrape recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("$RECIPES_V1_PATH/scrape") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiScrapeRecipe(url = "https://example.com/recipe"))
            }
        }

    @Test
    fun `scrape without a url`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("$RECIPES_V1_PATH/scrape") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiScrapeRecipe(url = ""))
            }
        }
}
