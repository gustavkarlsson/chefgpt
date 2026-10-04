package se.gustavkarlsson.chefgpt.api.recipes.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-scrape-recipe")
data class ApiScrapeRecipe(
    val url: String,
)
