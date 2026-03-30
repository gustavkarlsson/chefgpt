package se.gustavkarlsson.chefgpt.recipes

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
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
) : ToolSet,
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

    @Tool
    @LLMDescription(
        "Search for recipes using a natural language query with optional filters for cuisine, diet, " +
            "intolerances, meal type, and more. Returns a list of matching recipes.",
    )
    suspend fun searchRecipes(
        @LLMDescription("Natural language recipe search query, e.g. 'pasta carbonara'.")
        query: String,
        @LLMDescription("Cuisine type to filter by, e.g. 'italian', 'mexican', 'thai'.")
        cuisine: String = "",
        @LLMDescription("Diet to filter by, e.g. 'vegetarian', 'vegan', 'gluten free', 'ketogenic'.")
        diet: String = "",
        @LLMDescription("Intolerances to filter by, e.g. 'dairy', 'egg', 'gluten', 'peanut', 'soy', 'shellfish'.")
        intolerances: String = "",
        @LLMDescription("Meal type to filter by, e.g. 'breakfast', 'lunch', 'dinner', 'snack', 'dessert'.")
        type: String = "",
        @LLMDescription("Maximum ready time in minutes.")
        maxReadyTime: Int = 0,
        @LLMDescription("The maximum number of results to return (1-100).")
        number: Int = 10,
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

    @Tool
    @LLMDescription(
        "Search for recipes that fall within given nutritional ranges. " +
            "All nutrient bounds are optional; only set the ones you care about.",
    )
    suspend fun searchRecipesByNutrients(
        @LLMDescription("Minimum calories per serving.")
        minCalories: Int = 0,
        @LLMDescription("Maximum calories per serving.")
        maxCalories: Int = 0,
        @LLMDescription("Minimum protein in grams per serving.")
        minProtein: Int = 0,
        @LLMDescription("Maximum protein in grams per serving.")
        maxProtein: Int = 0,
        @LLMDescription("Minimum carbohydrates in grams per serving.")
        minCarbs: Int = 0,
        @LLMDescription("Maximum carbohydrates in grams per serving.")
        maxCarbs: Int = 0,
        @LLMDescription("Minimum fat in grams per serving.")
        minFat: Int = 0,
        @LLMDescription("Maximum fat in grams per serving.")
        maxFat: Int = 0,
        @LLMDescription("The maximum number of results to return (1-100).")
        number: Int = 10,
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

    @Tool
    @LLMDescription(
        "Find recipes that use as many of the given ingredients as possible " +
            "and require as few additional ingredients as possible.",
    )
    suspend fun searchRecipesByIngredients(
        @LLMDescription("Ingredients that you have at home.")
        ingredients: List<String>,
        @LLMDescription("The maximum number of results to return (1-100).")
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

    @Tool
    @LLMDescription(
        "Fetch full information for multiple recipes at once by their IDs. " +
            "More efficient than calling getRecipeInformation repeatedly.",
    )
    suspend fun getRecipeInformationBulk(
        @LLMDescription("List of recipe IDs to fetch.")
        ids: List<Int>,
        @LLMDescription("Whether to include nutrition data in the response.")
        includeNutrition: Boolean = false,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/informationBulk") {
                parameter("ids", ids.joinToString(","))
                parameter("includeNutrition", includeNutrition)
            }.body()

    @Tool
    @LLMDescription("Find recipes that are similar to a given recipe.")
    suspend fun getSimilarRecipes(
        @LLMDescription("The ID of the recipe to find similar recipes for.")
        id: Int,
        @LLMDescription("The maximum number of similar recipes to return (1-100).")
        number: Int = 10,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/similar") {
                parameter("number", number)
            }.body()

    @Tool
    @LLMDescription(
        "Retrieve a set of random popular recipes. Optionally filter by tags such as diet or meal type.",
    )
    suspend fun getRandomRecipes(
        @LLMDescription(
            "Comma-separated tags to filter random recipes, e.g. 'vegetarian,dessert'. " +
                "Can include diet labels, meal types, or cuisines.",
        )
        tags: String = "",
        @LLMDescription("The number of random recipes to return (1-100).")
        number: Int = 10,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/random") {
                if (tags.isNotBlank()) parameter("tags", tags)
                parameter("number", number)
            }.body()

    @Tool
    @LLMDescription(
        "Autocomplete a partial recipe name query. " +
            "Useful for suggesting recipe titles as the user types.",
    )
    suspend fun autocompleteRecipeSearch(
        @LLMDescription("The partial recipe name to complete, e.g. 'chick'.")
        query: String,
        @LLMDescription("The maximum number of suggestions to return (1-25).")
        number: Int = 10,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/autocomplete") {
                parameter("query", query)
                parameter("number", number)
            }.body()

    @Tool
    @LLMDescription(
        "Generate a short human-readable summary of a recipe by its ID. " +
            "Includes key highlights such as diet labels, ready time, and a brief description.",
    )
    suspend fun summarizeRecipe(
        @LLMDescription("The ID of the recipe to summarize.")
        id: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/summary")
            .body()

    @Tool
    @LLMDescription(
        "Get the taste profile of a recipe by its ID. " +
            "Returns scores for sweetness, saltiness, sourness, bitterness, savoriness, and fattiness.",
    )
    suspend fun tasteById(
        @LLMDescription("The ID of the recipe.")
        id: Int,
        @LLMDescription("Whether to normalize all taste values to a scale of 0–1.")
        normalize: Boolean = true,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/tasteWidget.json") {
                parameter("normalize", normalize)
            }.body()

    @Tool
    @LLMDescription("Get the equipment needed to prepare a recipe by its ID.")
    suspend fun equipmentById(
        @LLMDescription("The ID of the recipe.")
        id: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/equipmentWidget.json")
            .body()

    @Tool
    @LLMDescription(
        "Get the price breakdown of a recipe by its ID. " +
            "Returns cost per ingredient and total estimated cost.",
    )
    suspend fun priceBreakdownById(
        @LLMDescription("The ID of the recipe.")
        id: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/priceBreakdownWidget.json")
            .body()

    @Tool
    @LLMDescription("Get the ingredient list for a recipe by its ID, including amounts and units.")
    suspend fun ingredientsById(
        @LLMDescription("The ID of the recipe.")
        id: Int,
        @LLMDescription("The measurement system to use: 'us' or 'metric'.")
        measure: String = "metric",
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/ingredientWidget.json") {
                parameter("measure", measure)
            }.body()

    @Tool
    @LLMDescription("Get the detailed nutrition information for a recipe by its ID.")
    suspend fun nutritionById(
        @LLMDescription("The ID of the recipe.")
        id: Int,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/$id/nutritionWidget.json")
            .body()

    @Tool
    @LLMDescription(
        "Parse and analyze raw recipe instructions text. " +
            "Returns structured steps with the ingredients and equipment used in each step.",
    )
    suspend fun getAnalyzedRecipeInstructions(
        @LLMDescription("The plain-text recipe instructions to analyze.")
        instructions: String,
    ): String =
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

    @Tool
    @LLMDescription(
        "Extract a recipe from an external website URL. " +
            "Parses title, ingredients, instructions, and optionally nutrition data.",
    )
    suspend fun extractRecipeFromWebsite(
        @LLMDescription("The URL of the page that contains the recipe.")
        url: String,
        @LLMDescription("Whether to force extraction even if the site is not supported.")
        forceExtraction: Boolean = false,
        @LLMDescription("Whether to analyze the recipe and add extra information such as nutrition.")
        analyze: Boolean = false,
        @LLMDescription("Whether to include nutrition data in the response.")
        includeNutrition: Boolean = false,
        @LLMDescription("Whether to include taste data in the response.")
        includeTaste: Boolean = false,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/extract") {
                parameter("url", url)
                parameter("forceExtraction", forceExtraction)
                parameter("analyze", analyze)
                parameter("includeNutrition", includeNutrition)
                parameter("includeTaste", includeTaste)
            }.body()

    @Tool
    @LLMDescription(
        "Classify the cuisine of a recipe based on its title and ingredient list. " +
            "Returns the most likely cuisine and a confidence score.",
    )
    suspend fun classifyCuisine(
        @LLMDescription("The title of the recipe.")
        title: String,
        @LLMDescription("A newline-separated list of ingredients, e.g. '3 cups flour\\n1 tsp salt'.")
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

    @Tool
    @LLMDescription(
        "Estimate the nutrition of a dish from its name alone, without a full recipe. " +
            "Returns calories, protein, fat, and carbohydrates.",
    )
    suspend fun estimateNutritionByDishName(
        @LLMDescription("The name of the dish to estimate nutrition for, e.g. 'spaghetti bolognese'.")
        title: String,
    ): String =
        client
            .get("https://api.spoonacular.com/recipes/guessNutrition") {
                parameter("title", title)
            }.body()

    @Tool
    @LLMDescription(
        "Analyze a food image from a URL to estimate its nutrition. " +
            "Returns identified foods and their nutritional information.",
    )
    suspend fun estimateNutritionFromImage(
        @LLMDescription("The publicly accessible URL of the food image to analyze.")
        imageUrl: String,
    ): String =
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
