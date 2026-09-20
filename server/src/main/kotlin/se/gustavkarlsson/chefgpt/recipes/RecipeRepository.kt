package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.util.RepoSyncer

/**
 * Owns the recipe-modification rule: a recipe's modification takes its place until it is kept
 * as a copy or overwrites the original. [RecipePersistence] stores and reads recipes whole;
 * every decision about how modify / overwrite / copy mutate state lives here, once.
 */
class RecipeRepository(
    private val persistence: RecipePersistence,
) {
    private val syncer = RepoSyncer<UserId>()

    suspend fun getRecipe(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe? = persistence.get(userId, id)

    suspend fun getRecipeSummaries(userId: UserId): List<ApiRecipeSummary> = persistence.listSummaries(userId)

    fun streamRecipeSummaries(userId: UserId): Flow<List<ApiRecipeSummary>> =
        syncer
            .notifications(userId)
            .map { getRecipeSummaries(userId) }
            .distinctUntilChanged()

    suspend fun saveRecipe(
        userId: UserId,
        recipe: NewRecipe,
    ): ApiRecipe {
        val saved = persistence.insert(userId, recipe, favorite = false, modifiedFrom = null)
        syncer.notifyChange(userId)
        return saved
    }

    // Stores the non-null parts of the update as a modified version of the recipe, which
    // takes the recipe's place until it is kept as a copy or overwrites the recipe.
    // Modifying a recipe that is already a modification updates it in place.
    // Returns null if no recipe matched.
    suspend fun modifyRecipe(
        userId: UserId,
        id: RecipeId,
        update: RecipeUpdate,
    ): ApiRecipe? {
        val base = persistence.get(userId, id) ?: return null
        val modified =
            if (base.modifiedFrom != null) {
                persistence.replace(userId, base.applyUpdate(update))
            } else {
                val existing =
                    persistence
                        .listSummaries(userId)
                        .firstOrNull { it.modifiedFrom == base.id }
                        ?.let { persistence.get(userId, it.id) }
                if (existing != null) {
                    persistence.replace(userId, existing.applyUpdate(update))
                } else {
                    persistence.insert(
                        userId,
                        base.applyUpdate(update).toNewRecipe(),
                        favorite = base.favorite,
                        modifiedFrom = base.id,
                    )
                }
            }
        if (modified != null) syncer.notifyChange(userId)
        return modified
    }

    // Deletes the recipe this one was modified from, keeping this one in its place.
    // Returns null if no recipe matched or it is not a modification.
    suspend fun overwriteOriginal(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe? {
        val modification = persistence.get(userId, id) ?: return null
        val originalId = modification.modifiedFrom ?: return null
        persistence.delete(userId, originalId) // Detaches the modification
        val overwritten = persistence.get(userId, id)
        if (overwritten != null) syncer.notifyChange(userId)
        return overwritten
    }

    // Keeps the recipe alongside the one it was modified from.
    // Returns null if no recipe matched or it is not a modification.
    suspend fun saveAsCopy(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe? {
        val modification = persistence.get(userId, id) ?: return null
        if (modification.modifiedFrom == null) return null
        val copy = persistence.replace(userId, modification.copy(modifiedFrom = null))
        if (copy != null) syncer.notifyChange(userId)
        return copy
    }

    // Returns the updated recipe, or null if no recipe matched.
    suspend fun setFavorite(
        userId: UserId,
        id: RecipeId,
        favorite: Boolean,
    ): ApiRecipe? {
        val recipe = persistence.get(userId, id) ?: return null
        val updated = persistence.replace(userId, recipe.copy(favorite = favorite))
        if (updated != null) syncer.notifyChange(userId)
        return updated
    }

    // Returns true if a recipe was deleted, false if no recipe matched.
    suspend fun deleteRecipe(
        userId: UserId,
        id: RecipeId,
    ): Boolean {
        val deleted = persistence.delete(userId, id)
        if (deleted) syncer.notifyChange(userId)
        return deleted
    }
}
