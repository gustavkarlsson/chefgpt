package se.gustavkarlsson.chefgpt.recipes

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.sessions.SessionId

private val log = Logger.withTag("${ApiRecipeRepository::class.simpleName}")

class ApiRecipeRepository(
    private val client: ChefGptClient,
) : RecipeRepository {
    override suspend fun streamSummaries(sessionId: SessionId): Flow<List<RecipeSummary>> =
        client
            .listenToRecipeSummaries(sessionId)
            .map { summaries -> summaries.map { it.toRecipeSummary() } }

    override suspend fun get(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError> =
        client
            .getRecipe(sessionId, recipeId)
            .map { it.toRecipe() }
            .onOk { log.i { "Got recipe: $recipeId" } }
            .onErr { log.e { "Failed to get recipe: $recipeId" } }

    override suspend fun setFavorite(
        sessionId: SessionId,
        recipeId: RecipeId,
        favorite: Boolean,
    ): Result<Unit, ClientError> =
        client
            .setRecipeFavorite(sessionId, recipeId, favorite)
            .onOk { log.i { "Set favorite=$favorite on recipe: $recipeId" } }
            .onErr { log.e { "Failed to set favorite=$favorite on recipe: $recipeId" } }

    override suspend fun overwriteOriginal(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError> =
        client
            .overwriteOriginalRecipe(sessionId, recipeId)
            .map { it.toRecipe() }
            .onOk { log.i { "Overwrote the recipe modified by: $recipeId" } }
            .onErr { log.e { "Failed to overwrite the recipe modified by: $recipeId" } }

    override suspend fun saveAsCopy(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Recipe, ClientError> =
        client
            .saveRecipeAsCopy(sessionId, recipeId)
            .map { it.toRecipe() }
            .onOk { log.i { "Saved recipe as a copy: $recipeId" } }
            .onErr { log.e { "Failed to save recipe as a copy: $recipeId" } }

    override suspend fun delete(
        sessionId: SessionId,
        recipeId: RecipeId,
    ): Result<Unit, ClientError> =
        client
            .deleteRecipe(sessionId, recipeId)
            .onOk { log.i { "Deleted recipe: $recipeId" } }
            .onErr { log.e { "Failed to delete recipe: $recipeId" } }
}

private fun formatAmount(
    value: String,
    unit: String?,
): String = if (unit == null) value else "$value $unit"

private fun ApiRecipeSummary.toRecipeSummary(): RecipeSummary =
    RecipeSummary(id, title, imageUrl, favorite, modifiedFrom)

private fun ApiRecipe.toRecipe(): Recipe =
    Recipe(
        id = id,
        title = title,
        imageUrl = imageUrl,
        favorite = favorite,
        modifiedFrom = modifiedFrom,
        description = description,
        preparationDuration = preparationDuration,
        cookingDuration = cookingDuration,
        duration = duration,
        servings = servings,
        steps = steps,
        ingredients = ingredients.map { Ingredient(it.name, formatAmount(it.value, it.unit)) },
        nutrients = nutrients.map { Nutrient(it.name, formatAmount(it.value, it.unit)) },
    )
