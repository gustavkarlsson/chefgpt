package se.gustavkarlsson.chefgpt.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

class SpoonacularClient(
    apiKey: String,
) : ToolSet,
    AutoCloseable {
    private val client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
            }
            defaultRequest {
                header("x-api-key", apiKey)
            }
        }

    @Tool
    @LLMDescription(
        "Find recipes that use as many of the given ingredients as possible" +
            "and require as few additional ingredients as possible.",
    )
    suspend fun findByIngredients(
        @LLMDescription("Ingredients that you have at home")
        ingredients: List<String>,
        @LLMDescription("The maximum number of results to return (1-100)")
        resultCount: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/findByIngredients") {
                parameter("ingredients", ingredients)
                parameter("number", resultCount)
                parameter("ranking", 2)
                parameter("ignorePantry", true)
            }.body()

    @Tool
    @LLMDescription(
        "Use a recipe id to get full information about a recipe," +
            "such as ingredients, nutrition, diet and allergen information, etc.",
    )
    suspend fun getRecipeInformation(
        @LLMDescription("The id of the recipe.")
        id: Int,
        @LLMDescription(
            "Include nutrition data in the recipe information." +
                "Nutrition data is per serving." +
                "If you want the nutrition data for the entire recipe," +
                "just multiply by the number of servings.",
        )
        includeNutrition: Boolean = false,
        @LLMDescription("Add a wine pairing to the recipe.")
        addWinePairing: Boolean = false,
        @LLMDescription("Add taste data to the recipe.")
        addTasteData: Boolean = false,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/information") {
                parameter("includeNutrition", includeNutrition)
                parameter("addWinePairing", addWinePairing)
                parameter("addTasteData", addTasteData)
            }.body()

    override fun close() {
        client.close()
    }
}
