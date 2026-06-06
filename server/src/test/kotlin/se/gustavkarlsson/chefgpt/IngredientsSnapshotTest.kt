package se.gustavkarlsson.chefgpt

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class IngredientsSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun unauthenticated() =
        snapshotTestApplication(snapshotContext) { client ->
            client.get("/ingredients")
        }

    @Test
    fun `delete ingredient`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            val ingredients = addIngredients(sessionId, "tomato", "basil")
            val tomatoId = ingredients.first { it.name == "tomato" }.id

            client.delete("/ingredients/$tomatoId") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete ingredient not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("/ingredients/11111111-1111-1111-1111-111111111111") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete ingredient unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.delete("/ingredients/tomato")
        }

    @Test
    fun `put ingredient new`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.put("/ingredients/tomato") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `put ingredient existing`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            addIngredients(sessionId, "tomato")

            client.put("/ingredients/tomato") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `put ingredient unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.put("/ingredients/tomato")
        }
}
