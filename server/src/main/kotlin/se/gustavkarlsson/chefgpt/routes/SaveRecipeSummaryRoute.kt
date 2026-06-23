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
import se.gustavkarlsson.chefgpt.api.ApiSaveSpoonacularRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeClient
import se.gustavkarlsson.chefgpt.recipes.RecipeSummaryStore
import se.gustavkarlsson.chefgpt.requireSession

fun Route.saveRecipeSummaryRoute() {
    post("/recipe-summaries") {
        val recipeSummaryStore = get<RecipeSummaryStore>()
        val recipeClient = get<RecipeClient>()
        val json = get<Json>()
        val userId = call.requireSession().user.id
        val spoonacularId = call.receive<ApiSaveSpoonacularRecipe>().spoonacularId

        val lookedUp = lookUpRecipeSummary(recipeClient, json, spoonacularId.value)
        if (lookedUp == null) {
            return@post call.respond(
                HttpStatusCode.NotFound,
                ApiError("recipe-not-found", "Recipe not found"),
            )
        }

        val added = recipeSummaryStore.addRecipeSummary(userId, lookedUp.title, spoonacularId, lookedUp.imageUrl)
        call.respond(HttpStatusCode.Created, added)
    }
}

private data class LookedUpRecipeSummary(
    val title: String,
    val imageUrl: String?,
)

// TODO Don't parse the raw JSON ad hoc like this. RecipeClient should expose a
// typed lookup instead of returning a JSON string meant for the LLM.
private suspend fun lookUpRecipeSummary(
    recipeClient: RecipeClient,
    json: Json,
    spoonacularId: Long,
): LookedUpRecipeSummary? {
    val info = json.parseToJsonElement(recipeClient.getRecipeInformation(spoonacularId)).jsonObject
    val title = info["title"]?.jsonPrimitive?.contentOrNull ?: return null
    val imageUrl = info["image"]?.jsonPrimitive?.contentOrNull
    return LookedUpRecipeSummary(title, imageUrl)
}
