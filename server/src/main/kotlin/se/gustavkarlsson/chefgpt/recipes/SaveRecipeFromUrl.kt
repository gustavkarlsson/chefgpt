package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.agent.convertrecipeunits.ConvertRecipeUnitsAgent
import se.gustavkarlsson.chefgpt.auth.UserId

/**
 * Scrapes a recipe from a website URL, hosts its image on the trusted host, converts its units to
 * the user's preferences, and saves it. Returns null when no complete recipe could be scraped.
 */
class SaveRecipeFromUrl(
    private val scraper: RecipeScraper,
    private val imageRehoster: RecipeImageRehoster,
    private val repository: RecipeRepository,
    private val convertRecipeUnits: ConvertRecipeUnitsAgent,
) {
    suspend fun save(
        userId: UserId,
        url: String,
    ): Recipe? {
        val recipe = scraper.scrape(url, partialRecipe = null) ?: return null
        val hostedImageUrl = recipe.imageUrl?.let { imageUrl -> imageRehoster.rehost(imageUrl) }
        val withHostedImage = recipe.copy(imageUrl = hostedImageUrl)
        val convertedRecipe = convertRecipeUnits.convert(userId, withHostedImage)
        return repository.saveRecipe(userId, convertedRecipe)
    }
}
