package se.gustavkarlsson.chefgpt.recipes

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.auth.UserId

@Suppress("unused")
class RecipeSummaryStoreTools(
    private val store: RecipeSummaryStore,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Save a recipe to the user's saved recipe summaries. " +
            "Call this as soon as the user has chosen a recipe they want to keep.",
    )
    suspend fun saveRecipeSummary(
        @LLMDescription("The Spoonacular ID of the recipe.")
        spoonacularId: Long,
        @LLMDescription("The title of the recipe.")
        title: String,
        @LLMDescription("The URL of the recipe's cover image, or an empty string if not available.")
        imageUrl: String = "",
    ): ApiRecipeSummary =
        store.addRecipeSummary(
            userId,
            title,
            SpoonacularId(spoonacularId),
            imageUrl.ifEmpty {
                null
            },
        )
}

fun RecipeSummaryStore.toTools(userId: UserId): ToolSet = RecipeSummaryStoreTools(this, userId)
