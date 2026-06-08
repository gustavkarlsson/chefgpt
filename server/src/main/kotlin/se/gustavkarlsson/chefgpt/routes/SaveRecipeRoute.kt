package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiSaveRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeClient
import se.gustavkarlsson.chefgpt.recipes.RecipeStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.saveRecipeRoute() {
    post("/recipes") {
        val recipeStore = get<RecipeStore>()
        val recipeClient = get<RecipeClient>()
        val json = get<Json>()
        val userId = call.requireSession().user.id
        val spoonacularId = call.receive<ApiSaveRecipe>().spoonacularId

        val recipe = lookUpRecipe(recipeClient, json, spoonacularId)
        if (recipe == null) {
            return@post call.respond(
                HttpStatusCode.NotFound,
                ApiError("recipe-not-found", "Recipe not found"),
            )
        }

        val added = recipeStore.addRecipe(userId, recipe.title, recipe.url, recipe.imageUrl)
        call.respond(HttpStatusCode.Created, added)
    }
}

private data class LookedUpRecipe(
    val title: String,
    val url: String,
    val imageUrl: String?,
)

// TODO Don't parse the raw JSON ad hoc like this. RecipeClient should expose a
// typed lookup instead of returning a JSON string meant for the LLM.
private suspend fun lookUpRecipe(
    recipeClient: RecipeClient,
    json: Json,
    spoonacularId: Int,
): LookedUpRecipe? {
    val info = json.parseToJsonElement(recipeClient.getRecipeInformation(spoonacularId)).jsonObject
    val title = info["title"]?.jsonPrimitive?.contentOrNull ?: return null
    val url = (info["sourceUrl"] ?: info["spoonacularSourceUrl"])?.jsonPrimitive?.contentOrNull ?: return null
    val imageUrl = info["image"]?.jsonPrimitive?.contentOrNull
    return LookedUpRecipe(title, url, imageUrl)
}
