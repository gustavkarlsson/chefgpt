package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import kotlin.math.floor
import kotlin.time.Duration.Companion.minutes

// Looks up recipes from the recipe client, which only speaks raw JSON meant for the LLM.
class RecipeLookup(
    private val recipeClient: RecipeClient,
    private val json: Json,
) {
    suspend fun lookUp(spoonacularId: SpoonacularId): NewRecipe? {
        val (infoElement, stepsElement) =
            coroutineScope {
                val info = async { recipeClient.getRecipeInformation(spoonacularId.value, includeNutrition = true) }
                val steps = async { recipeClient.getAnalyzedInstructionsById(spoonacularId.value) }
                json.parseToJsonElement(info.await()) to json.parseToJsonElement(steps.await())
            }
        val info = infoElement.jsonObject
        val title = info["title"]?.jsonPrimitive?.contentOrNull ?: return null
        return NewRecipe(
            title = title,
            steps = stepsElement.toSteps(),
            spoonacularId = spoonacularId,
            imageUrl = info["image"]?.jsonPrimitive?.contentOrNull?.let(::ImageUrl),
            description = info["summary"]?.jsonPrimitive?.contentOrNull,
            preparationDuration = info.minutesOrNull("preparationMinutes"),
            cookingDuration = info.minutesOrNull("cookingMinutes"),
            duration = info.minutesOrNull("readyInMinutes"),
            ingredients =
                info["extendedIngredients"]
                    ?.jsonArray
                    ?.mapNotNull { ingredient ->
                        val amount = ingredient.jsonObject.toAmountOrNull() ?: return@mapNotNull null
                        ApiRecipeIngredient(amount.name, amount.value, amount.unit)
                    }.orEmpty(),
            nutrients =
                info["nutrition"]
                    ?.jsonObject
                    ?.get("nutrients")
                    ?.jsonArray
                    ?.mapNotNull { nutrient ->
                        val amount = nutrient.jsonObject.toAmountOrNull() ?: return@mapNotNull null
                        ApiNutrient(amount.name, amount.value, amount.unit)
                    }.orEmpty(),
        )
    }
}

private class Amount(
    val name: String,
    val value: String,
    val unit: String?,
)

private fun JsonObject.toAmountOrNull(): Amount? {
    val name = get("name")?.jsonPrimitive?.contentOrNull ?: return null
    val amount = get("amount")?.jsonPrimitive?.doubleOrNull ?: return null
    val unit = get("unit")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    return Amount(name, formatValue(amount), unit)
}

private fun JsonObject.minutesOrNull(key: String) =
    get(key)
        ?.jsonPrimitive
        ?.intOrNull
        ?.takeIf { it > 0 }
        ?.minutes

private fun JsonElement.toSteps(): List<String> =
    jsonArray.flatMap { instructionSet ->
        instructionSet.jsonObject["steps"]
            ?.jsonArray
            ?.mapNotNull { step -> step.jsonObject["step"]?.jsonPrimitive?.contentOrNull }
            .orEmpty()
    }

private fun formatValue(amount: Double): String =
    if (amount == floor(amount)) amount.toInt().toString() else "%.1f".format(amount)
