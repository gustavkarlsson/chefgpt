package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.chefGptJson
import se.gustavkarlsson.chefgpt.files.HtmlLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val JSON_LD_PAGE =
    """
    <html><head><script type="application/ld+json">
    {
        "@type": "Recipe",
        "name": "Carbonara",
        "recipeIngredient": ["200 g spaghetti", "2 eggs"],
        "recipeInstructions": ["Cook the spaghetti.", "Mix the eggs."]
    }
    </script></head><body>Content</body></html>
    """.trimIndent()

class JsonLdScrapeRecipeTest {
    @Test
    fun `returns the recipe parsed from the fetched page`() =
        runTest {
            val scraper =
                JsonLdScrapeRecipe(
                    FixedTextLoader(JSON_LD_PAGE),
                    RecipeJsonLdParser(chefGptJson(strict = false)),
                )

            val recipe = assertNotNull(scraper.scrape("https://example.com/recipe", null))

            assertEquals("Carbonara", recipe.title)
        }

    @Test
    fun `returns null when the page could not be fetched`() =
        runTest {
            val scraper =
                JsonLdScrapeRecipe(
                    FixedTextLoader(null),
                    RecipeJsonLdParser(chefGptJson(strict = false)),
                )

            val recipe = scraper.scrape("https://example.com/recipe", null)

            assertNull(recipe)
        }

    @Test
    fun `returns null when the page has no JSON-LD`() =
        runTest {
            val scraper =
                JsonLdScrapeRecipe(
                    FixedTextLoader("no json-ld here"),
                    RecipeJsonLdParser(chefGptJson(strict = false)),
                )

            val recipe = scraper.scrape("https://example.com/recipe", null)

            assertNull(recipe)
        }
}

private class FixedTextLoader(
    private val html: String?,
) : HtmlLoader {
    override suspend fun loadText(url: String): String? = html
}
