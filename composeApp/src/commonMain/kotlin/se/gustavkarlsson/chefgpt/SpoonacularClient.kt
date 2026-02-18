package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.tools.Tool
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
import kotlinx.serialization.builtins.ListSerializer
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
    val extendedName: String? = null
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
    val usedIngredients: List<Ingredient>
)

@Serializable
data class FindByIngredientsArgs(
    val ingredients: List<String>,
    val resultCount: Int,
)

class SpoonacularClient(private val apiKey: String) : AutoCloseable {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 1000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun findByIngredients(
        ingredients: List<String>,
        resultCount: Int,
    ): List<Recipe> {
        return client.get("https://api.spoonacular.com/recipes/findByIngredients") {
            header("x-api-key", apiKey)

            parameter("ingredients", ingredients)
            parameter("number", resultCount)
            parameter("ranking", 2)
            parameter("ignorePantry", true)
        }.body()
    }

    override fun close() {
        client.close()
    }
}

fun SpoonacularClient.asTools(): List<Tool<*, *>> = listOf(
    object : Tool<FindByIngredientsArgs, List<Recipe>>(
        argsSerializer = FindByIngredientsArgs.serializer(),
        resultSerializer = ListSerializer(Recipe.serializer()),
        name = "findByIngredients",
        description = "Find recipes that use as many of the given ingredients as possible and require as few additional ingredients as possible. ingredients: Ingredients that you have at home. resultCount: The maximum number of results to return (1-100)."
    ) {
        override suspend fun execute(args: FindByIngredientsArgs): List<Recipe> =
            findByIngredients(args.ingredients, args.resultCount)
    },
)