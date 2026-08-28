package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.Flow
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId

interface RecipeStore {
    suspend fun getRecipe(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe?

    suspend fun getRecipeSummaries(userId: UserId): List<ApiRecipeSummary>

    fun streamRecipeSummaries(userId: UserId): Flow<List<ApiRecipeSummary>>

    suspend fun saveRecipe(
        userId: UserId,
        recipe: NewRecipe,
    ): ApiRecipe

    // Stores the non-null parts of the update as a modified version of the recipe, which
    // takes the recipe's place until it is kept as a copy or overwrites the recipe.
    // Modifying a recipe that is already a modification updates it in place.
    // Returns null if no recipe matched.
    suspend fun modifyRecipe(
        userId: UserId,
        id: RecipeId,
        update: RecipeUpdate,
    ): ApiRecipe?

    // Deletes the recipe this one was modified from, keeping this one in its place.
    // Returns null if no recipe matched or it is not a modification.
    suspend fun overwriteOriginal(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe?

    // Keeps the recipe alongside the one it was modified from.
    // Returns null if no recipe matched or it is not a modification.
    suspend fun saveAsCopy(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe?

    // Returns the updated recipe, or null if no recipe matched.
    suspend fun setFavorite(
        userId: UserId,
        id: RecipeId,
        favorite: Boolean,
    ): ApiRecipe?

    // Returns true if a recipe was deleted, false if no recipe matched.
    suspend fun deleteRecipe(
        userId: UserId,
        id: RecipeId,
    ): Boolean
}
