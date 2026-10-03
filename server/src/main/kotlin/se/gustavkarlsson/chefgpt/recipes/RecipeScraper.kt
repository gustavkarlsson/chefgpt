package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.agent.convertrecipeunits.ConvertRecipeUnitsAgent
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.auth.UserId

// Scrapes a recipe from a website URL, converts its units to the user's preferences, and saves it.
// Returns null if no recipe could be extracted.
class RecipeScraper(
    private val lookup: RecipeLookup,
    private val repository: RecipeRepository,
    private val convertRecipeUnits: ConvertRecipeUnitsAgent,
) {
    suspend fun scrape(
        userId: UserId,
        url: String,
    ): ApiRecipe? {
        val recipe = lookup.scrape(url) ?: return null
        val convertedRecipe = convertRecipeUnits.convert(userId, recipe)
        return repository.saveRecipe(userId, convertedRecipe)
    }
}
