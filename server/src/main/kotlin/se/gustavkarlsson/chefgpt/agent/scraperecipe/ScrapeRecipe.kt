package se.gustavkarlsson.chefgpt.agent.scraperecipe

import se.gustavkarlsson.chefgpt.recipes.NewRecipe

fun interface ScrapeRecipe {
    suspend operator fun invoke(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe
}
