package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiError
import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeSummaryId
import se.gustavkarlsson.chefgpt.recipes.RecipeClient
import se.gustavkarlsson.chefgpt.recipes.RecipeSummaryStore
import se.gustavkarlsson.chefgpt.requireSession
import kotlin.time.Duration.Companion.minutes

private fun formatValue(amount: Double): String =
    if (amount == kotlin.math.floor(amount)) amount.toInt().toString() else "%.1f".format(amount)

fun Route.getRecipeRoute() {
    get("/recipe-summaries/{id}") {
        val recipeSummaryStore = get<RecipeSummaryStore>()
        val recipeClient = get<RecipeClient>()
        val json = get<Json>()
        val userId = call.requireSession().user.id
        val id =
            RecipeSummaryId.parseOrNull(call.parameters.getOrFail("id"))
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("invalid-recipe-summary-id", "Invalid recipe summary id"),
                )

        val summary =
            recipeSummaryStore.getRecipeSummary(userId, id)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ApiError("recipe-summary-not-found", "Recipe summary not found"),
                )

        // TODO Don't parse the raw JSON ad hoc like this. RecipeClient should expose a
        // typed lookup instead of returning a JSON string meant for the LLM.
        val spoonacularId = summary.spoonacularId.value
        val (infoElement, stepsElement) =
            coroutineScope {
                val info = async { recipeClient.getRecipeInformation(spoonacularId, includeNutrition = true) }
                val steps = async { recipeClient.getAnalyzedInstructionsById(spoonacularId) }
                json.parseToJsonElement(info.await()) to json.parseToJsonElement(steps.await())
            }

        val info = infoElement.jsonObject
        val title = info["title"]?.jsonPrimitive?.contentOrNull ?: summary.title
        val imageUrl = info["image"]?.jsonPrimitive?.contentOrNull?.let(::ImageUrl)
        val description = info["summary"]?.jsonPrimitive?.contentOrNull
        val duration =
            info["readyInMinutes"]
                ?.jsonPrimitive
                ?.intOrNull
                ?.takeIf { it > 0 }
                ?.minutes
        val preparationDuration =
            info["preparationMinutes"]
                ?.jsonPrimitive
                ?.intOrNull
                ?.takeIf { it > 0 }
                ?.minutes
        val cookingDuration =
            info["cookingMinutes"]
                ?.jsonPrimitive
                ?.intOrNull
                ?.takeIf { it > 0 }
                ?.minutes

        val steps =
            stepsElement.jsonArray.flatMap { instructionSet ->
                instructionSet.jsonObject["steps"]
                    ?.jsonArray
                    ?.mapNotNull { step ->
                        step.jsonObject["step"]?.jsonPrimitive?.contentOrNull
                    }.orEmpty()
            }

        val ingredients =
            info["extendedIngredients"]
                ?.jsonArray
                ?.mapNotNull { ingredient ->
                    val name =
                        ingredient.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                            ?: return@mapNotNull null
                    val amount =
                        ingredient.jsonObject["amount"]?.jsonPrimitive?.doubleOrNull
                            ?: return@mapNotNull null
                    val unit =
                        ingredient.jsonObject["unit"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            .orEmpty()
                    ApiRecipeIngredient(name = name, value = formatValue(amount), unit = unit)
                }.orEmpty()

        val nutrients =
            info["nutrition"]
                ?.jsonObject
                ?.get("nutrients")
                ?.jsonArray
                ?.mapNotNull { nutrient ->
                    val name = nutrient.jsonObject["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val amount = nutrient.jsonObject["amount"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                    val unit =
                        nutrient.jsonObject["unit"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            .orEmpty()
                    ApiNutrient(name = name, value = formatValue(amount), unit = unit)
                }.orEmpty()

        call.respond(
            HttpStatusCode.OK,
            ApiRecipe(
                id = summary.id,
                spoonacularId = summary.spoonacularId,
                title = title,
                imageUrl = imageUrl,
                steps = steps,
                description = description,
                preparationDuration = preparationDuration,
                cookingDuration = cookingDuration,
                duration = duration,
                ingredients = ingredients,
                nutrients = nutrients,
            ),
        )
    }
}
