package se.gustavkarlsson.chefgpt.agent.scraperecipe

import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.recipes.NewRecipe

class FakeScrapeRecipeAgent : ScrapeRecipeAgent {
    override suspend fun scrape(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe =
        NewRecipe(
            title = "Fake scraped recipe",
            steps = listOf("Fake step."),
            imageUrl = null,
            description = null,
            preparationDuration = null,
            cookingDuration = null,
            duration = null,
            servings = null,
            ingredients = listOf(ApiRecipeIngredient("flour", "2", "cups")),
            nutrients = emptyList(),
            spoonacularId = null,
        )
}
