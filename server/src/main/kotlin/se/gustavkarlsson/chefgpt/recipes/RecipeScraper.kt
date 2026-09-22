package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.auth.UserId

// Scrapes a recipe from a website URL and saves it. Returns null if no recipe could be extracted.
class RecipeScraper(
    private val lookup: RecipeLookup,
    private val repository: RecipeRepository,
) {
    suspend fun scrape(
        userId: UserId,
        url: String,
    ): ApiRecipe? {
        val recipe = lookup.scrape(url) ?: return null
        return repository.saveRecipe(userId, recipe)
    }
}
