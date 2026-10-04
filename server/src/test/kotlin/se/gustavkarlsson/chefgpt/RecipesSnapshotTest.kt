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
import se.gustavkarlsson.chefgpt.api.common.SpoonacularId
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiRecipeUpdate
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiSaveSpoonacularRecipe
import se.gustavkarlsson.chefgpt.api.recipes.v1.RECIPES_V1_PATH
import se.gustavkarlsson.chefgpt.recipes.RecipeUpdate
import se.gustavkarlsson.chefgpt.recipes.TestRecipeRepository
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

private const val MISSING_ID = "11111111-1111-1111-1111-111111111111"

@ExtendWith(SnapshotExtension::class)
class RecipesSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext
    private val recipeRepository = TestRecipeRepository()

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `save recipe unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post(RECIPES_V1_PATH) {
                contentType(ContentType.Application.Json)
                setBody(ApiSaveSpoonacularRecipe(SpoonacularId(716429L)))
            }
        }

    @Test
    fun `save recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post(RECIPES_V1_PATH) {
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

            client.post(RECIPES_V1_PATH) {
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

            client.get("$RECIPES_V1_PATH/${recipe.id}") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `get recipe not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.get("$RECIPES_V1_PATH/$MISSING_ID") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `get recipe invalid id`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.get("$RECIPES_V1_PATH/not-a-uuid") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `get recipe unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.get("$RECIPES_V1_PATH/$MISSING_ID")
        }

    @Test
    fun `favorite recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))

            client.patch("$RECIPES_V1_PATH/${recipe.id}") {
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

            client.patch("$RECIPES_V1_PATH/${recipe.id}") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiRecipeUpdate(favorite = false))
            }
        }

    @Test
    fun `favorite recipe not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.patch("$RECIPES_V1_PATH/$MISSING_ID") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiRecipeUpdate(favorite = true))
            }
        }

    @Test
    fun `overwrite original recipe`() =
        snapshotTestApplication(snapshotContext, extraKoinModules = listOf(recipeRepository.koinModule)) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))
            val modified = recipeRepository.modifyRecipe(recipe.id, RecipeUpdate(title = "Vegetarian carbonara"))

            client.post("$RECIPES_V1_PATH/${modified.id}/overwrite-original") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `overwrite original recipe that is not modified`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))

            client.post("$RECIPES_V1_PATH/${recipe.id}/overwrite-original") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `overwrite original recipe not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("$RECIPES_V1_PATH/$MISSING_ID/overwrite-original") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `save recipe as copy`() =
        snapshotTestApplication(snapshotContext, extraKoinModules = listOf(recipeRepository.koinModule)) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))
            val modified = recipeRepository.modifyRecipe(recipe.id, RecipeUpdate(title = "Vegetarian carbonara"))

            client.post("$RECIPES_V1_PATH/${modified.id}/save-as-copy") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `save recipe as copy that is not modified`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))

            client.post("$RECIPES_V1_PATH/${recipe.id}/save-as-copy") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `save recipe as copy invalid id`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.post("$RECIPES_V1_PATH/not-a-uuid/save-as-copy") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `save recipe as copy unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("$RECIPES_V1_PATH/$MISSING_ID/save-as-copy")
        }

    @Test
    fun `delete recipe`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()
            val recipe = saveRecipe(sessionId, SpoonacularId(716429L))

            client.delete("$RECIPES_V1_PATH/${recipe.id}") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete recipe not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("$RECIPES_V1_PATH/$MISSING_ID") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete recipe invalid id`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.delete("$RECIPES_V1_PATH/not-a-uuid") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `delete recipe unauthenticated`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.delete("$RECIPES_V1_PATH/$MISSING_ID")
        }
}
