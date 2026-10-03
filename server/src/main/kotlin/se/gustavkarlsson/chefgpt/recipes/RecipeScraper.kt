package se.gustavkarlsson.chefgpt.recipes

/**
 * Scrapes the recipe at url. partialRecipe holds whatever an earlier scrape attempt already
 * extracted, to reuse as hints. Returns null when no recipe could be extracted.
 */
interface RecipeScraper {
    suspend fun scrape(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe?
}
