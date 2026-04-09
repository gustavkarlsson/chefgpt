package se.gustavkarlsson.chefgpt

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.DefaultJson
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.util.sseTyped
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
    fun `list ingredients empty`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            // FIXME slapshot doesn't handle SSE bodies
            client.sseTyped<List<String>>(
                json = DefaultJson,
                eventType = "ingredients",
                request = {
                    url("/ingredients")
                    header("Session-Id", sessionId)
                },
            ) { _, incoming ->
                incoming.first()
            }
        }

    @Test
    fun `list ingredients with some`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            addIngredients(sessionId, "tomato", "basil")

            // FIXME slapshot doesn't handle SSE bodies
            client.sseTyped<List<String>>(
                json = DefaultJson,
                eventType = "ingredients",
                request = {
                    url("/ingredients")
                    header("Session-Id", sessionId)
                },
            ) { _, incoming ->
                incoming.first()
            }
        }

    @Test
    fun `delete ingredient`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            addIngredients(sessionId, "tomato", "basil")

            client.delete("/ingredients/tomato") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete ingredient not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("/ingredients/tomato") {
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
