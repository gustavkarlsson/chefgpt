package se.gustavkarlsson.chefgpt.recipes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.toSummary
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.toApiRecipe
import java.util.concurrent.ConcurrentHashMap

class InMemoryRecipePersistence(
    private val storage: ConcurrentHashMap<UserId, MutableStateFlow<Map<RecipeId, ApiRecipe>>> =
        ConcurrentHashMap(),
) : RecipePersistence {
    override suspend fun get(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe? = storage[userId]?.value?.get(id)

    override suspend fun insert(
        userId: UserId,
        recipe: NewRecipe,
        favorite: Boolean,
        modifiedFrom: RecipeId?,
    ): ApiRecipe {
        val saved = recipe.toApiRecipe(id = RecipeId.random(), favorite = favorite, modifiedFrom = modifiedFrom)
        storedRecipes(userId).updateAndGet { it + (saved.id to saved) }
        return saved
    }

    override suspend fun replace(
        userId: UserId,
        recipe: ApiRecipe,
    ): ApiRecipe? {
        var replaced: ApiRecipe? = null
        storedRecipes(userId).updateAndGet { current ->
            if (current.containsKey(recipe.id)) {
                replaced = recipe
                current + (recipe.id to recipe)
            } else {
                current
            }
        }
        return replaced
    }

    override suspend fun delete(
        userId: UserId,
        id: RecipeId,
    ): Boolean {
        var removed = false
        storedRecipes(userId).updateAndGet { current ->
            removed = current.containsKey(id)
            if (removed) {
                current
                    .mapValues { (_, recipe) ->
                        if (recipe.modifiedFrom == id) recipe.copy(modifiedFrom = null) else recipe
                    }.minus(id)
            } else {
                current
            }
        }
        return removed
    }

    override suspend fun listSummaries(userId: UserId): List<ApiRecipeSummary> = storage[userId]?.value.toSummaries()

    private fun storedRecipes(userId: UserId): MutableStateFlow<Map<RecipeId, ApiRecipe>> =
        storage.getOrPut(userId) { MutableStateFlow(emptyMap()) }
}

// Recipes that have been modified are represented by their modification, which takes their
// place until it is kept as a copy or overwrites them.
private fun Map<RecipeId, ApiRecipe>?.toSummaries(): List<ApiRecipeSummary> {
    val recipes = this?.values.orEmpty()
    val modifiedIds = recipes.mapNotNull { it.modifiedFrom }.toSet()
    return recipes.filter { it.id !in modifiedIds }.map { it.toSummary() }
}
