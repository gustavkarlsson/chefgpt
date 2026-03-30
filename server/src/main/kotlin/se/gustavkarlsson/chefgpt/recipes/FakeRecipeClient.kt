package se.gustavkarlsson.chefgpt.recipes

@Suppress("LongParameterList")
class FakeRecipeClient : RecipeClient {
    override suspend fun searchRecipes(
        query: String,
        cuisine: String,
        diet: String,
        intolerances: String,
        type: String,
        maxReadyTime: Int,
        number: Int,
    ): String =
        """{"results":[{"id":716429,"title":"Pasta with Garlic, Scallions, Cauliflower & Breadcrumbs","image":"https://img.spoonacular.com/recipes/716429-312x231.jpg","imageType":"jpg"},{"id":715538,"title":"What to make for dinner tonight?? Bruschetta Style Pork & Pasta","image":"https://img.spoonacular.com/recipes/715538-312x231.jpg","imageType":"jpg"}],"offset":0,"number":2,"totalResults":86}"""

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
        """[{"id":557315,"title":"Chicken Fajita Stuffed Bell Pepper","image":"https://img.spoonacular.com/recipes/557315-312x231.jpg","imageType":"jpg","calories":300,"protein":"25g","fat":"10g","carbs":"20g"},{"id":580660,"title":"Healthy Grilled Chicken Salad","image":"https://img.spoonacular.com/recipes/580660-312x231.jpg","imageType":"jpg","calories":280,"protein":"30g","fat":"8g","carbs":"15g"}]"""

    override suspend fun searchRecipesByIngredients(
        ingredients: List<String>,
        resultCount: Int,
    ): String =
        """[{"id":641803,"title":"Easy & Delicious Berry Smoothie","image":"https://img.spoonacular.com/recipes/641803-312x231.jpg","imageType":"jpg","usedIngredientCount":2,"missedIngredientCount":1,"likes":12}]"""

    override suspend fun getRecipeInformation(
        id: Int,
        includeNutrition: Boolean,
        addWinePairing: Boolean,
        addTasteData: Boolean,
    ): String =
        """{"id":$id,"title":"Spaghetti Carbonara","readyInMinutes":30,"servings":4,"sourceUrl":"https://example.com/carbonara","image":"https://img.spoonacular.com/recipes/716429-556x370.jpg","summary":"A classic Italian pasta dish with eggs, cheese, pancetta, and pepper.","instructions":"<ol><li>Cook pasta.</li><li>Fry pancetta.</li><li>Mix eggs and cheese.</li><li>Combine and serve.</li></ol>","extendedIngredients":[{"id":1,"name":"spaghetti","amount":400.0,"unit":"g"},{"id":2,"name":"pancetta","amount":200.0,"unit":"g"},{"id":3,"name":"eggs","amount":4.0,"unit":""},{"id":4,"name":"parmesan cheese","amount":100.0,"unit":"g"},{"id":5,"name":"black pepper","amount":1.0,"unit":"tsp"}],"vegetarian":false,"vegan":false,"glutenFree":false,"dairyFree":false,"cuisines":["Italian"],"dishTypes":["lunch","main course","dinner"]}"""

    override suspend fun getRecipeInformationBulk(
        ids: List<Int>,
        includeNutrition: Boolean,
    ): String =
        """[{"id":${ids.firstOrNull() ?: 1},"title":"Spaghetti Carbonara","readyInMinutes":30,"servings":4,"summary":"Classic Italian pasta.","vegetarian":false,"vegan":false}]"""

    override suspend fun getSimilarRecipes(
        id: Int,
        number: Int,
    ): String =
        """[{"id":209128,"title":"Cacio e Pepe","readyInMinutes":25,"servings":2},{"id":227961,"title":"Pasta Aglio e Olio","readyInMinutes":20,"servings":4}]"""

    override suspend fun getRandomRecipes(
        tags: String,
        number: Int,
    ): String =
        """{"recipes":[{"id":782585,"title":"Cannellini Bean and Asparagus Salad with Mushrooms","readyInMinutes":25,"servings":6,"summary":"A light and healthy salad.","vegetarian":true,"vegan":true}]}"""

    override suspend fun autocompleteRecipeSearch(
        query: String,
        number: Int,
    ): String =
        """[{"id":716429,"title":"Chicken Parmesan"},{"id":715538,"title":"Chicken Tikka Masala"},{"id":716432,"title":"Chicken Alfredo"}]"""

    override suspend fun summarizeRecipe(id: Int): String =
        """{"id":$id,"title":"Spaghetti Carbonara","summary":"Spaghetti Carbonara is a classic Italian dish that takes about 30 minutes to prepare. It serves 4 and is perfect for a weeknight dinner. Each serving contains approximately 450 calories."}"""

    override suspend fun tasteById(
        id: Int,
        normalize: Boolean,
    ): String =
        """{"sweetness":0.15,"saltiness":0.45,"sourness":0.05,"bitterness":0.02,"savoriness":0.85,"fattiness":0.65}"""

    override suspend fun equipmentById(id: Int): String =
        """{"equipment":[{"name":"pot","image":"pot.png"},{"name":"frying pan","image":"pan.png"},{"name":"bowl","image":"bowl.jpg"}]}"""

    override suspend fun priceBreakdownById(id: Int): String =
        """{"ingredients":[{"name":"spaghetti","price":1.50},{"name":"pancetta","price":3.20},{"name":"eggs","price":0.80},{"name":"parmesan","price":2.50}],"totalCost":8.00,"totalCostPerServing":2.00}"""

    override suspend fun ingredientsById(
        id: Int,
        measure: String,
    ): String =
        """{"ingredients":[{"name":"spaghetti","amount":{"metric":{"value":400.0,"unit":"g"}}},{"name":"pancetta","amount":{"metric":{"value":200.0,"unit":"g"}}},{"name":"eggs","amount":{"metric":{"value":4.0,"unit":""}}},{"name":"parmesan cheese","amount":{"metric":{"value":100.0,"unit":"g"}}}]}"""

    override suspend fun nutritionById(id: Int): String =
        """{"calories":"450","carbs":"55g","fat":"18g","protein":"22g","nutrients":[{"name":"Calories","amount":450.0,"unit":"kcal","percentOfDailyNeeds":22.5},{"name":"Fat","amount":18.0,"unit":"g","percentOfDailyNeeds":27.7},{"name":"Protein","amount":22.0,"unit":"g","percentOfDailyNeeds":44.0},{"name":"Carbohydrates","amount":55.0,"unit":"g","percentOfDailyNeeds":18.3}]}"""

    override suspend fun getAnalyzedRecipeInstructions(instructions: String): String =
        """{"parsedInstructions":[{"name":"","steps":[{"number":1,"step":"Cook pasta according to package directions.","ingredients":[{"id":1,"name":"pasta"}],"equipment":[{"id":1,"name":"pot"}]},{"number":2,"step":"Drain and serve.","ingredients":[],"equipment":[{"id":2,"name":"colander"}]}]}]}"""

    override suspend fun extractRecipeFromWebsite(
        url: String,
        forceExtraction: Boolean,
        analyze: Boolean,
        includeNutrition: Boolean,
        includeTaste: Boolean,
    ): String =
        """{"title":"Extracted Recipe","readyInMinutes":45,"servings":4,"instructions":"Step 1: Prepare ingredients. Step 2: Cook. Step 3: Serve.","extendedIngredients":[{"name":"chicken breast","amount":500.0,"unit":"g"},{"name":"olive oil","amount":2.0,"unit":"tbsp"}]}"""

    override suspend fun classifyCuisine(
        title: String,
        ingredientList: String,
    ): String = """{"cuisine":"Italian","cuisines":["Italian","European"],"confidence":0.85}"""

    override suspend fun estimateNutritionByDishName(title: String): String =
        """{"calories":{"value":450,"unit":"calories","confidenceRange95Percent":{"min":350.0,"max":550.0}},"fat":{"value":18,"unit":"g","confidenceRange95Percent":{"min":12.0,"max":24.0}},"protein":{"value":22,"unit":"g","confidenceRange95Percent":{"min":16.0,"max":28.0}},"carbs":{"value":55,"unit":"g","confidenceRange95Percent":{"min":40.0,"max":70.0}}}"""

    override suspend fun estimateNutritionFromImage(imageUrl: String): String =
        """{"category":{"name":"pizza","probability":0.92},"nutrition":{"calories":{"value":266,"unit":"calories"},"fat":{"value":10,"unit":"g"},"protein":{"value":11,"unit":"g"},"carbs":{"value":33,"unit":"g"}},"recipes":[{"id":654959,"title":"Pasta With Tuna","url":"https://example.com/pasta-tuna"}]}"""
}
