package se.gustavkarlsson.chefgpt.recipes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import se.gustavkarlsson.chefgpt.api.common.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap

class InMemoryRecipePersistence(
    private val storage: ConcurrentHashMap<UserId, MutableStateFlow<Map<RecipeId, Recipe>>> =
        ConcurrentHashMap(),
) : RecipePersistence {
    override suspend fun get(
        userId: UserId,
        id: RecipeId,
    ): Recipe? = storage[userId]?.value?.get(id)

    override suspend fun insert(
        userId: UserId,
        recipe: NewRecipe,
        favorite: Boolean,
        modifiedFrom: RecipeId?,
    ): Recipe {
        val saved = recipe.toRecipe(id = RecipeId.random(), favorite = favorite, modifiedFrom = modifiedFrom)
        storedRecipes(userId).updateAndGet { it + (saved.id to saved) }
        return saved
    }

    override suspend fun replace(
        userId: UserId,
        recipe: Recipe,
    ): Recipe? {
        var replaced: Recipe? = null
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

    override suspend fun listSummaries(userId: UserId): List<RecipeSummary> = storage[userId]?.value.toSummaries()

    private fun storedRecipes(userId: UserId): MutableStateFlow<Map<RecipeId, Recipe>> =
        storage.getOrPut(userId) { MutableStateFlow(emptyMap()) }
}

// Recipes that have been modified are represented by their modification, which takes their
// place until it is kept as a copy or overwrites them.
private fun Map<RecipeId, Recipe>?.toSummaries(): List<RecipeSummary> {
    val recipes = this?.values.orEmpty()
    val modifiedIds = recipes.mapNotNull { it.modifiedFrom }.toSet()
    return recipes.filter { it.id !in modifiedIds }.map { it.toSummary() }
}
