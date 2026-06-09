package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.DurationSerializer
import kotlin.time.Duration

@Serializable
data class ApiRecipe(
    val id: RecipeSummaryId,
    val spoonacularId: SpoonacularId,
    val title: String,
    val imageUrl: ImageUrl? = null,
    val steps: List<String>,
    val description: String? = null,
    @Serializable(with = DurationSerializer::class)
    val preparationDuration: Duration? = null,
    @Serializable(with = DurationSerializer::class)
    val cookingDuration: Duration? = null,
    @Serializable(with = DurationSerializer::class)
    val duration: Duration? = null,
)
