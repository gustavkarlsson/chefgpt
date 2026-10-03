package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.files.HtmlLoader

/**
 * Scrapes the recipe from the page's embedded JSON-LD. Returns whatever it found, complete or
 * not, so a later scraper can reuse it as hints.
 */
class JsonLdScrapeRecipe(
    private val htmlLoader: HtmlLoader,
    private val jsonLdParser: RecipeJsonLdParser,
) : RecipeScraper {
    override suspend fun scrape(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe? {
        val html = htmlLoader.loadText(url) ?: return null
        return jsonLdParser.parseRecipe(html)
    }
}
