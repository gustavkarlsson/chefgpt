package se.gustavkarlsson.chefgpt.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Ingredient(
    val aisle: String,
    val amount: Double,
    val id: Int,
    val image: String,
    val meta: List<String>,
    val name: String,
    val original: String,
    val originalName: String,
    val unit: String,
    val unitLong: String,
    val unitShort: String,
    val extendedName: String? = null,
)

@Serializable
data class ExtendedIngredient(
    val aisle: String,
    val amount: Double,
    val consistency: String,
    val id: Int,
    val image: String,
    val meta: List<String>,
    val name: String,
    val original: String,
    val originalName: String,
    val unit: String,
)

@Serializable
data class Recipe(
    val id: Int,
    val image: String,
    val imageType: String,
    val likes: Int,
    val missedIngredientCount: Int,
    val missedIngredients: List<Ingredient>,
    val title: String,
    val unusedIngredients: List<Ingredient>,
    val usedIngredientCount: Int,
    val usedIngredients: List<Ingredient>,
)

@Serializable
data class RecipeDetails(
    val id: Int,
    val title: String,
    val image: String,
    val servings: Int,
    val readyInMinutes: Int,
    val sourceUrl: String,
    val spoonacularSourceUrl: String,
    val healthScore: Double,
    val spoonacularScore: Double,
    val pricePerServing: Double,
    val cheap: Boolean,
    val dairyFree: Boolean,
    val glutenFree: Boolean,
    val ketogenic: Boolean? = null,
    val lowFodmap: Boolean,
    val sustainable: Boolean,
    val vegan: Boolean,
    val vegetarian: Boolean,
    val whole30: Boolean? = null,
    val extendedIngredients: List<ExtendedIngredient>,
    val summary: String,
    val instructions: String? = null,
)

class SpoonacularClient(
    private val apiKey: String,
) : ToolSet,
    AutoCloseable {
    private val client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 1000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
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
    ): List<Recipe> =
        client
            .get("https://api.spoonacular.com/recipes/findByIngredients") {
                header("x-api-key", apiKey)

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
    ): RecipeDetails =
        client
            .get("https://api.spoonacular.com/recipes/$id/information") {
                header("x-api-key", apiKey)

                parameter("includeNutrition", includeNutrition)
                parameter("addWinePairing", addWinePairing)
                parameter("addTasteData", addTasteData)
            }.body()

    override fun close() {
        client.close()
    }
}
