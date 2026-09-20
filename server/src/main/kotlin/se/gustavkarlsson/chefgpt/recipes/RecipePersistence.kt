package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId

/**
 * The persistence seam behind [RecipeRepository]. Stores recipes whole and lists their
 * summaries; the modification lifecycle rule lives in the repository, not here. Each method
 * is atomic on its own.
 */
interface RecipePersistence {
    suspend fun get(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe?

    // A first save has neither a favorite nor a modification, so both are passed explicitly.
    suspend fun insert(
        userId: UserId,
        recipe: NewRecipe,
        favorite: Boolean,
        modifiedFrom: RecipeId?,
    ): ApiRecipe

    // Rewrites the recipe in place, including its steps, ingredients and nutrients.
    // Returns null if no recipe matched.
    suspend fun replace(
        userId: UserId,
        recipe: ApiRecipe,
    ): ApiRecipe?

    // Deletes the recipe and detaches any modification of it, which then stands alone.
    suspend fun delete(
        userId: UserId,
        id: RecipeId,
    ): Boolean

    // Summaries where a modification stands in for the recipe it was modified from.
    suspend fun listSummaries(userId: UserId): List<ApiRecipeSummary>
}
