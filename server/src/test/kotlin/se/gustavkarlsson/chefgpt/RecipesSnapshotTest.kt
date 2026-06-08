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
import se.gustavkarlsson.chefgpt.api.ApiSaveRecipe
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class RecipesSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `save recipe unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/recipes") {
                contentType(ContentType.Application.Json)
                setBody(ApiSaveRecipe(716429))
            }
        }

    @Test
    fun `save recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/recipes") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiSaveRecipe(716429))
            }
        }

    @Test
    fun `delete recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, 716429)

            client.delete("/recipes/${recipe.id}") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete recipe not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("/recipes/11111111-1111-1111-1111-111111111111") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete recipe invalid id`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("/recipes/not-a-uuid") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete recipe unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.delete("/recipes/11111111-1111-1111-1111-111111111111")
        }
}
