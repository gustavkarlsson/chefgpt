package se.gustavkarlsson.chefgpt.ingredients

import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap

class InMemoryIngredientStore(
    private val storage: ConcurrentHashMap<UserId, MutableList<String>> = ConcurrentHashMap(),
) : IngredientStore {
    override suspend fun getIngredients(userId: UserId): List<String> = storage[userId].orEmpty().toList()

    override suspend fun addIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<String> {
        val storedIngredients = storage.getOrPut(userId) { mutableListOf() }
        val normalized = ingredients.map { it.trim().lowercase() }
        val added = normalized.filter { it !in storedIngredients }
        storedIngredients += added
        return added
    }

    override suspend fun removeIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<String> {
        val storedIngredients = storage.getOrPut(userId) { mutableListOf() }
        return ingredients.filter { storedIngredients.remove(it) }
    }

    override suspend fun clearIngredients(userId: UserId): List<String> {
        val storedIngredients = storage.getOrPut(userId) { mutableListOf() }
        val removed = storedIngredients.toList()
        storedIngredients.clear()
        return removed
    }
}
