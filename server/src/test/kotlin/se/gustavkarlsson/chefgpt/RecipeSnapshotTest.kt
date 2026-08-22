package se.gustavkarlsson.chefgpt

import io.ktor.client.request.get
import io.ktor.client.request.header
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class RecipeSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `get recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipeSummary = saveRecipeSummary(sessionId, SpoonacularId(716429L))

            client.get("/recipes/${recipeSummary.id}") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `get recipe not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.get("/recipes/11111111-1111-1111-1111-111111111111") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `get recipe invalid id`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.get("/recipes/not-a-uuid") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `get recipe unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.get("/recipes/11111111-1111-1111-1111-111111111111")
        }
}
