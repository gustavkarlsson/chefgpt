package se.gustavkarlsson.chefgpt

import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.ApiSaveSpoonacularRecipe
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class RecipeSummariesSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `save recipe summary unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/recipe-summaries") {
                contentType(ContentType.Application.Json)
                setBody(ApiSaveSpoonacularRecipe(SpoonacularId(716429L)))
            }
        }

    @Test
    fun `save recipe summary`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/recipe-summaries") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiSaveSpoonacularRecipe(SpoonacularId(716429L)))
            }
        }

    @Test
    fun `delete recipe summary`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipeSummary = saveRecipeSummary(sessionId, SpoonacularId(716429L))

            client.delete("/recipe-summaries/${recipeSummary.id}") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete recipe summary not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("/recipe-summaries/11111111-1111-1111-1111-111111111111") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete recipe summary invalid id`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("/recipe-summaries/not-a-uuid") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete recipe summary unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.delete("/recipe-summaries/11111111-1111-1111-1111-111111111111")
        }
}
