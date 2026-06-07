package se.gustavkarlsson.chefgpt

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.ApiIngredientUpdate
import se.gustavkarlsson.chefgpt.api.ApiNewIngredient
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

            val ingredients = createIngredients(sessionId, "tomato", "basil")
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
    fun `create ingredient new`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/ingredients") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiNewIngredient("tomato"))
            }
        }

    @Test
    fun `create ingredient existing`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            createIngredients(sessionId, "tomato")

            client.post("/ingredients") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiNewIngredient("tomato"))
            }
        }

    @Test
    fun `create ingredient unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/ingredients") {
                contentType(ContentType.Application.Json)
                setBody(ApiNewIngredient("tomato"))
            }
        }

    @Test
    fun `patch ingredient out of store`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            val tomatoId = createIngredients(sessionId, "tomato").single().id

            client.patch("/ingredients/$tomatoId") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiIngredientUpdate(inInventory = false))
            }
        }

    @Test
    fun `patch ingredient back into store`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            val tomatoId = createIngredients(sessionId, "tomato").single().id
            setIngredientInventory(sessionId, tomatoId, inInventory = false)

            client.patch("/ingredients/$tomatoId") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiIngredientUpdate(inInventory = true))
            }
        }

    @Test
    fun `patch ingredient not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.patch("/ingredients/11111111-1111-1111-1111-111111111111") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiIngredientUpdate(inInventory = true))
            }
        }

    @Test
    fun `patch ingredient unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.patch("/ingredients/11111111-1111-1111-1111-111111111111") {
                contentType(ContentType.Application.Json)
                setBody(ApiIngredientUpdate(inInventory = true))
            }
        }
}
