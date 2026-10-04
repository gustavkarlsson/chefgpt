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
import se.gustavkarlsson.chefgpt.api.ingredients.v1.ApiIngredientUpdate
import se.gustavkarlsson.chefgpt.api.ingredients.v1.ApiNewIngredient
import se.gustavkarlsson.chefgpt.api.ingredients.v1.INGREDIENTS_V1_PATH
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
            client.get(INGREDIENTS_V1_PATH)
        }

    @Test
    fun `delete ingredient`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            val ingredients = createIngredients(sessionId, "tomato", "basil")
            val tomatoId = ingredients.first { it.name == "tomato" }.id

            client.delete("$INGREDIENTS_V1_PATH/$tomatoId") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete ingredient not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("$INGREDIENTS_V1_PATH/11111111-1111-1111-1111-111111111111") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete ingredient unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.delete("$INGREDIENTS_V1_PATH/tomato")
        }

    @Test
    fun `create ingredient new`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post(INGREDIENTS_V1_PATH) {
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

            client.post(INGREDIENTS_V1_PATH) {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiNewIngredient("tomato"))
            }
        }

    @Test
    fun `create ingredient unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post(INGREDIENTS_V1_PATH) {
                contentType(ContentType.Application.Json)
                setBody(ApiNewIngredient("tomato"))
            }
        }

    @Test
    fun `patch ingredient out of store`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            val tomatoId = createIngredients(sessionId, "tomato").single().id

            client.patch("$INGREDIENTS_V1_PATH/$tomatoId") {
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

            client.patch("$INGREDIENTS_V1_PATH/$tomatoId") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiIngredientUpdate(inInventory = true))
            }
        }

    @Test
    fun `patch ingredient not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.patch("$INGREDIENTS_V1_PATH/11111111-1111-1111-1111-111111111111") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiIngredientUpdate(inInventory = true))
            }
        }

    @Test
    fun `patch ingredient unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.patch("$INGREDIENTS_V1_PATH/11111111-1111-1111-1111-111111111111") {
                contentType(ContentType.Application.Json)
                setBody(ApiIngredientUpdate(inInventory = true))
            }
        }
}
