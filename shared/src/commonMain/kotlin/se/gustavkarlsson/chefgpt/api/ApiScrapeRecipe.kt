package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-scrape-recipe")
data class ApiScrapeRecipe(
    val url: String,
)
