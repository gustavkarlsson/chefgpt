package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap

class InMemoryIngredientStore(
    private val storage: ConcurrentHashMap<UserId, MutableStateFlow<Set<String>>> = ConcurrentHashMap(),
) : IngredientStore {
    override suspend fun getIngredients(userId: UserId): List<String> = storage[userId]?.value.orEmpty().toList()

    override fun streamIngredients(userId: UserId): Flow<List<String>> =
        storage
            .getOrPut(userId) { MutableStateFlow(emptySet()) }
            .map { it.toList() }

    override suspend fun addIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<String> {
        val storedIngredients = storage.getOrPut(userId) { MutableStateFlow(emptySet()) }
        val normalized = ingredients.map { it.trim().lowercase() }
        val preUpdate = storedIngredients.getAndUpdate { it + normalized }
        val added = normalized - preUpdate
        return added.toList()
    }

    override suspend fun removeIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<String> {
        val storedIngredients = storage.getOrPut(userId) { MutableStateFlow(emptySet()) }
        val preUpdate = storedIngredients.getAndUpdate { it - ingredients.toSet() }
        val removed = ingredients intersect preUpdate
        return removed.toList()
    }

    override suspend fun clearIngredients(userId: UserId): List<String> {
        val storedIngredients = storage.getOrPut(userId) { MutableStateFlow(emptySet()) }
        val preUpdate = storedIngredients.getAndUpdate { emptySet() }
        return preUpdate.toList()
    }
}
