package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.toSummary
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap

class InMemoryRecipeStore(
    private val storage: ConcurrentHashMap<UserId, MutableStateFlow<Map<RecipeId, ApiRecipe>>> =
        ConcurrentHashMap(),
) : RecipeStore {
    override suspend fun getRecipe(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe? = storage[userId]?.value?.get(id)

    override suspend fun getRecipeSummaries(userId: UserId): List<ApiRecipeSummary> =
        storage[userId]?.value.toSummaries()

    override fun streamRecipeSummaries(userId: UserId): Flow<List<ApiRecipeSummary>> =
        storedRecipes(userId).map { recipes -> recipes.toSummaries() }

    override suspend fun saveRecipe(
        userId: UserId,
        recipe: NewRecipe,
    ): ApiRecipe {
        val saved = recipe.toApiRecipe(id = RecipeId.random(), favorite = false)
        storedRecipes(userId).updateAndGet { it + (saved.id to saved) }
        return saved
    }

    override suspend fun modifyRecipe(
        userId: UserId,
        id: RecipeId,
        update: RecipeUpdate,
    ): ApiRecipe? {
        val recipes = storedRecipes(userId).value
        val base = recipes[id] ?: return null
        val existingModification =
            if (base.modifiedFrom != null) base else recipes.values.firstOrNull { it.modifiedFrom == base.id }
        if (existingModification != null) {
            return update(userId, existingModification.id) { it.applyUpdate(update) }
        }
        val modification =
            base
                .applyUpdate(update)
                .toNewRecipe()
                .toApiRecipe(id = RecipeId.random(), favorite = base.favorite, modifiedFrom = base.id)
        storedRecipes(userId).updateAndGet { it + (modification.id to modification) }
        return modification
    }

    override suspend fun overwriteOriginal(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe? {
        val originalId = storedRecipes(userId).value[id]?.modifiedFrom ?: return null
        var overwritten: ApiRecipe? = null
        storedRecipes(userId).updateAndGet { current ->
            overwritten = current[id]?.copy(modifiedFrom = null) ?: return@updateAndGet current
            current - originalId + (id to overwritten)
        }
        return overwritten
    }

    override suspend fun saveAsCopy(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe? {
        if (storedRecipes(userId).value[id]?.modifiedFrom == null) return null
        return update(userId, id) { it.copy(modifiedFrom = null) }
    }

    override suspend fun setFavorite(
        userId: UserId,
        id: RecipeId,
        favorite: Boolean,
    ): ApiRecipe? = update(userId, id) { it.copy(favorite = favorite) }

    override suspend fun deleteRecipe(
        userId: UserId,
        id: RecipeId,
    ): Boolean {
        var removed = false
        storedRecipes(userId).updateAndGet { current ->
            removed = current.containsKey(id)
            current - id
        }
        return removed
    }

    private fun update(
        userId: UserId,
        id: RecipeId,
        transform: (ApiRecipe) -> ApiRecipe,
    ): ApiRecipe? {
        var updated: ApiRecipe? = null
        storedRecipes(userId).updateAndGet { current ->
            updated = current[id]?.let(transform)
            updated?.let { current + (id to it) } ?: current
        }
        return updated
    }

    private fun storedRecipes(userId: UserId): MutableStateFlow<Map<RecipeId, ApiRecipe>> =
        storage.getOrPut(userId) { MutableStateFlow(emptyMap()) }
}

// Recipes that have been modified are represented by their modification, which takes
// their place until it is kept as a copy or overwrites them.
private fun Map<RecipeId, ApiRecipe>?.toSummaries(): List<ApiRecipeSummary> {
    val recipes = this?.values.orEmpty()
    val modifiedIds = recipes.mapNotNull { it.modifiedFrom }.toSet()
    return recipes.filter { it.id !in modifiedIds }.map { it.toSummary() }
}
