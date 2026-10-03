package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.chefGptJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class RecipeJsonLdParserTest {
    private val parser = RecipeJsonLdParser(chefGptJson(strict = false))

    @Test
    fun `extracts a recipe from JSON-LD`() {
        val html =
            page(
                """
                {
                    "@context": "https://schema.org",
                    "@type": "Recipe",
                    "name": "Carbonara",
                    "description": "A classic pasta dish.",
                    "recipeIngredient": ["200 g spaghetti", "2 eggs"],
                    "recipeInstructions": [
                        {"@type": "HowToStep", "text": "Cook the spaghetti."},
                        {"@type": "HowToStep", "text": "Mix the eggs."}
                    ],
                    "image": "https://example.com/carbonara.jpg",
                    "prepTime": "PT15M",
                    "cookTime": "PT30M",
                    "totalTime": "PT45M",
                    "recipeYield": "4 servings"
                }
                """,
            )

        val recipe = assertNotNull(parser.parseRecipe(html))

        assertEquals("Carbonara", recipe.title)
        assertEquals("A classic pasta dish.", recipe.description)
        assertEquals(
            listOf(ApiRecipeIngredient("200 g spaghetti", "", null), ApiRecipeIngredient("2 eggs", "", null)),
            recipe.ingredients,
        )
        assertEquals(listOf("Cook the spaghetti.", "Mix the eggs."), recipe.steps)
        assertEquals(ImageUrl("https://example.com/carbonara.jpg"), recipe.imageUrl)
        assertEquals(15.minutes, recipe.preparationDuration)
        assertEquals(30.minutes, recipe.cookingDuration)
        assertEquals(45.minutes, recipe.duration)
        assertEquals(4..4, recipe.servings)
        assertNull(recipe.spoonacularId)
    }

    @Test
    fun `extracts a recipe nested in a graph`() {
        val html =
            page(
                """
                {
                    "@context": "https://schema.org",
                    "@graph": [
                        {"@type": "Article", "name": "About pasta"},
                        {
                            "@type": "Recipe",
                            "name": "Carbonara",
                            "recipeIngredient": ["200 g spaghetti"],
                            "recipeInstructions": ["Cook the spaghetti."]
                        }
                    ]
                }
                """,
            )

        val recipe = assertNotNull(parser.parseRecipe(html))

        assertEquals("Carbonara", recipe.title)
        assertEquals(listOf("Cook the spaghetti."), recipe.steps)
    }

    @Test
    fun `parses a yield range`() {
        val html =
            page(
                """
                {
                    "@type": "Recipe",
                    "name": "Carbonara",
                    "recipeIngredient": ["200 g spaghetti"],
                    "recipeInstructions": ["Cook the spaghetti."],
                    "recipeYield": "4-6 servings"
                }
                """,
            )

        val recipe = assertNotNull(parser.parseRecipe(html))

        assertEquals(4..6, recipe.servings)
    }

    @Test
    fun `returns null when the page has no recipe JSON-LD`() {
        val html = page("""{"@type": "Article", "name": "About pasta"}""")

        val recipe = parser.parseRecipe(html)

        assertNull(recipe)
    }

    @Test
    fun `returns null when the recipe has no name`() {
        val html =
            page(
                """
                {
                    "@type": "Recipe",
                    "recipeIngredient": ["200 g spaghetti"],
                    "recipeInstructions": ["Cook the spaghetti."]
                }
                """,
            )

        val recipe = parser.parseRecipe(html)

        assertNull(recipe)
    }

    @Test
    fun `skips a script that is not valid JSON`() {
        val html =
            """
            <html><head>
            <script type="application/ld+json">{this is not json</script>
            <script type="application/ld+json">
            {
                "@type": "Recipe",
                "name": "Carbonara",
                "recipeIngredient": ["200 g spaghetti"],
                "recipeInstructions": ["Cook the spaghetti."]
            }
            </script>
            </head><body>Content</body></html>
            """.trimIndent()

        val recipe = assertNotNull(parser.parseRecipe(html))

        assertEquals("Carbonara", recipe.title)
    }

    private fun page(jsonLd: String): String =
        """
        <html><head><script type="application/ld+json">
        ${jsonLd.trimIndent()}
        </script></head><body>Content</body></html>
        """.trimIndent()
}
