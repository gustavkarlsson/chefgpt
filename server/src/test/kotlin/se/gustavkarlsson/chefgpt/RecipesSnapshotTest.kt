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
import se.gustavkarlsson.chefgpt.api.ApiRecipeUpdate
import se.gustavkarlsson.chefgpt.api.ApiSaveSpoonacularRecipe
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.recipes.RecipeUpdate
import se.gustavkarlsson.chefgpt.recipes.TestRecipeStore
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

private const val MISSING_ID = "11111111-1111-1111-1111-111111111111"

@ExtendWith(SnapshotExtension::class)
class RecipesSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext
    private val recipeStore = TestRecipeStore()

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `save recipe unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/recipes") {
                contentType(ContentType.Application.Json)
                setBody(ApiSaveSpoonacularRecipe(SpoonacularId(716429L)))
            }
        }

    @Test
    fun `save recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/recipes") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiSaveSpoonacularRecipe(SpoonacularId(716429L)))
            }
        }

    @Test
    fun `save recipe twice`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            saveRecipe(sessionId, SpoonacularId(716429L))

            client.post("/recipes") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiSaveSpoonacularRecipe(SpoonacularId(716429L)))
            }
        }

    @Test
    fun `get recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))

            client.get("/recipes/${recipe.id}") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `get recipe not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.get("/recipes/$MISSING_ID") {
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
            client.get("/recipes/$MISSING_ID")
        }

    @Test
    fun `favorite recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))

            client.patch("/recipes/${recipe.id}") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiRecipeUpdate(favorite = true))
            }
        }

    @Test
    fun `unfavorite recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))
            setRecipeFavorite(sessionId, recipe.id, favorite = true)

            client.patch("/recipes/${recipe.id}") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiRecipeUpdate(favorite = false))
            }
        }

    @Test
    fun `favorite recipe not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.patch("/recipes/$MISSING_ID") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiRecipeUpdate(favorite = true))
            }
        }

    @Test
    fun `overwrite original recipe`() =
        snapshotTestApplication(snapshotContext, extraKoinModules = listOf(recipeStore.koinModule)) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))
            val modified = recipeStore.modifyRecipe(recipe.id, RecipeUpdate(title = "Vegetarian carbonara"))

            client.post("/recipes/${modified.id}/overwrite-original") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `overwrite original recipe that is not modified`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))

            client.post("/recipes/${recipe.id}/overwrite-original") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `overwrite original recipe not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/recipes/$MISSING_ID/overwrite-original") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `save recipe as copy`() =
        snapshotTestApplication(snapshotContext, extraKoinModules = listOf(recipeStore.koinModule)) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))
            val modified = recipeStore.modifyRecipe(recipe.id, RecipeUpdate(title = "Vegetarian carbonara"))

            client.post("/recipes/${modified.id}/save-as-copy") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `save recipe as copy that is not modified`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))

            client.post("/recipes/${recipe.id}/save-as-copy") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `save recipe as copy invalid id`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("/recipes/not-a-uuid/save-as-copy") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `save recipe as copy unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/recipes/$MISSING_ID/save-as-copy")
        }

    @Test
    fun `delete recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))

            client.delete("/recipes/${recipe.id}") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete recipe not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("/recipes/$MISSING_ID") {
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
            client.delete("/recipes/$MISSING_ID")
        }
}
