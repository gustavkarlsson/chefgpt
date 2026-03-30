package se.gustavkarlsson.chefgpt.recipes

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType

// TODO Verify functions
class SpoonacularClient(
    apiKey: String,
) : RecipeClient,
    AutoCloseable {
    private val client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
            }
            // TODO Install logging
            defaultRequest {
                header("x-api-key", apiKey)
            }
        }

    override suspend fun searchRecipes(
        query: String,
        cuisine: String,
        diet: String,
        intolerances: String,
        type: String,
        maxReadyTime: Int,
        number: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/complexSearch") {
                parameter("query", query)
                if (cuisine.isNotBlank()) parameter("cuisine", cuisine)
                if (diet.isNotBlank()) parameter("diet", diet)
                if (intolerances.isNotBlank()) parameter("intolerances", intolerances)
                if (type.isNotBlank()) parameter("type", type)
                if (maxReadyTime > 0) parameter("maxReadyTime", maxReadyTime)
                parameter("number", number)
            }.body()

    override suspend fun searchRecipesByNutrients(
        minCalories: Int,
        maxCalories: Int,
        minProtein: Int,
        maxProtein: Int,
        minCarbs: Int,
        maxCarbs: Int,
        minFat: Int,
        maxFat: Int,
        number: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/findByNutrients") {
                if (minCalories > 0) parameter("minCalories", minCalories)
                if (maxCalories > 0) parameter("maxCalories", maxCalories)
                if (minProtein > 0) parameter("minProtein", minProtein)
                if (maxProtein > 0) parameter("maxProtein", maxProtein)
                if (minCarbs > 0) parameter("minCarbs", minCarbs)
                if (maxCarbs > 0) parameter("maxCarbs", maxCarbs)
                if (minFat > 0) parameter("minFat", minFat)
                if (maxFat > 0) parameter("maxFat", maxFat)
                parameter("number", number)
            }.body()

    override suspend fun searchRecipesByIngredients(
        ingredients: List<String>,
        resultCount: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/findByIngredients") {
                parameter("ingredients", ingredients)
                parameter("number", resultCount)
                parameter("ranking", 2)
                parameter("ignorePantry", true)
            }.body()

    override suspend fun getRecipeInformation(
        id: Int,
        includeNutrition: Boolean,
        addWinePairing: Boolean,
        addTasteData: Boolean,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/information") {
                parameter("includeNutrition", includeNutrition)
                parameter("addWinePairing", addWinePairing)
                parameter("addTasteData", addTasteData)
            }.body()

    override suspend fun getRecipeInformationBulk(
        ids: List<Int>,
        includeNutrition: Boolean,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/informationBulk") {
                parameter("ids", ids.joinToString(","))
                parameter("includeNutrition", includeNutrition)
            }.body()

    override suspend fun getSimilarRecipes(
        id: Int,
        number: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/similar") {
                parameter("number", number)
            }.body()

    override suspend fun getRandomRecipes(
        tags: String,
        number: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/random") {
                if (tags.isNotBlank()) parameter("tags", tags)
                parameter("number", number)
            }.body()

    override suspend fun autocompleteRecipeSearch(
        query: String,
        number: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/autocomplete") {
                parameter("query", query)
                parameter("number", number)
            }.body()

    override suspend fun summarizeRecipe(id: Int): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/summary")
            .body()

    override suspend fun tasteById(
        id: Int,
        normalize: Boolean,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/tasteWidget.json") {
                parameter("normalize", normalize)
            }.body()

    override suspend fun equipmentById(id: Int): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/equipmentWidget.json")
            .body()

    override suspend fun priceBreakdownById(id: Int): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/priceBreakdownWidget.json")
            .body()

    override suspend fun ingredientsById(
        id: Int,
        measure: String,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/ingredientWidget.json") {
                parameter("measure", measure)
            }.body()

    override suspend fun nutritionById(id: Int): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/nutritionWidget.json")
            .body()

    override suspend fun getAnalyzedRecipeInstructions(instructions: String): String =
        client
            .post("https://api.spoonacular.com/recipes/analyzeInstructions") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("instructions", instructions)
                        },
                    ),
                )
            }.body()

    override suspend fun extractRecipeFromWebsite(
        url: String,
        forceExtraction: Boolean,
        analyze: Boolean,
        includeNutrition: Boolean,
        includeTaste: Boolean,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/extract") {
                parameter("url", url)
                parameter("forceExtraction", forceExtraction)
                parameter("analyze", analyze)
                parameter("includeNutrition", includeNutrition)
                parameter("includeTaste", includeTaste)
            }.body()

    override suspend fun classifyCuisine(
        title: String,
        ingredientList: String,
    ): String =
        client
            .post("https://api.spoonacular.com/recipes/cuisine") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("title", title)
                            append("ingredientList", ingredientList)
                        },
                    ),
                )
            }.body()

    override suspend fun estimateNutritionByDishName(title: String): String =
        client
            .get("https://api.spoonacular.com/recipes/guessNutrition") {
                parameter("title", title)
            }.body()

    override suspend fun estimateNutritionFromImage(imageUrl: String): String =
        client
            .post("https://api.spoonacular.com/food/images/analyze") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("imageUrl", imageUrl)
                        },
                    ),
                )
            }.body()

    override fun close() {
        client.close()
    }
}
