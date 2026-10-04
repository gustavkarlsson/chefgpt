package se.gustavkarlsson.chefgpt.recipes

/**
 * Scrapes the recipe through the recipe client's website extraction, returning whatever it
 * managed to extract, complete or not, so a later scraper can reuse it as hints. Its failures
 * propagate, so the failure shape survives a failed chain.
 */
class SpoonacularRecipeScraper(
    private val spoonacular: Spoonacular,
) : RecipeScraper {
    override suspend fun scrape(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe? = spoonacular.scrape(url)
}
