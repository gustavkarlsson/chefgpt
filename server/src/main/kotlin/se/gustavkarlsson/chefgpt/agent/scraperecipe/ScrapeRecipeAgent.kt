package se.gustavkarlsson.chefgpt.agent.scraperecipe

import se.gustavkarlsson.chefgpt.recipes.NewRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeScraper

/**
 * Extracts the recipe at url by reading the page itself. When an earlier scrape attempt partially
 * succeeded, partialRecipe holds what it found, to reuse as hints. Returns null when no complete
 * recipe could be extracted.
 */
interface ScrapeRecipeAgent : RecipeScraper {
    override suspend fun scrape(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe?
}
