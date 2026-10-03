package se.gustavkarlsson.chefgpt.recipes

// Recipe clients that fail in the ways the scrape fallback cares about.
class NoInstructionsClient : RecipeClient by FakeRecipeClient() {
    override suspend fun extractRecipeFromWebsite(
        url: String,
        forceExtraction: Boolean,
        analyze: Boolean,
        includeNutrition: Boolean,
        includeTaste: Boolean,
    ): String = """{"title":"No recipe here"}"""
}

class NoStepsClient : RecipeClient by FakeRecipeClient() {
    override suspend fun getAnalyzedRecipeInstructions(instructions: String): String = """{"parsedInstructions":[]}"""
}

class ThrowingExtractClient : RecipeClient by FakeRecipeClient() {
    override suspend fun extractRecipeFromWebsite(
        url: String,
        forceExtraction: Boolean,
        analyze: Boolean,
        includeNutrition: Boolean,
        includeTaste: Boolean,
    ): String = error("extract failed")
}
