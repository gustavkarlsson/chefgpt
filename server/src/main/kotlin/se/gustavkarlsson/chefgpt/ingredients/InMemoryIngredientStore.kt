package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock

class InMemoryIngredientStore(
    private val storage: ConcurrentHashMap<UserId, MutableStateFlow<Map<String, ApiIngredient>>> = ConcurrentHashMap(),
) : IngredientStore {
    override suspend fun getIngredients(userId: UserId): List<ApiIngredient> =
        storage[userId]
            ?.value
            ?.values
            ?.toList()
            .orEmpty()

    override fun streamIngredients(userId: UserId): Flow<List<ApiIngredient>> =
        storage
            .getOrPut(userId) { MutableStateFlow(emptyMap()) }
            .map { it.values.toList() }

    override suspend fun addIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient> {
        val now = Clock.System.now()
        val normalized = ingredients.map { it.trim().lowercase() }.distinct()
        val preUpdate =
            storedIngredients(userId).getAndUpdate { current ->
                current +
                    normalized
                        .filter { current[it]?.inInventory != true }
                        .associateWith { ApiIngredient(it, now, inInventory = true) }
            }
        return normalized
            .filter { preUpdate[it]?.inInventory != true }
            .map { ApiIngredient(it, now, inInventory = true) }
    }

    override suspend fun removeIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient> {
        val now = Clock.System.now()
        val normalized = ingredients.map { it.trim().lowercase() }.distinct()
        val preUpdate =
            storedIngredients(userId).getAndUpdate { current ->
                current +
                    normalized
                        .mapNotNull { current[it]?.takeIf(ApiIngredient::inInventory) }
                        .associate { it.name to it.copy(inInventory = false, lastModified = now) }
            }
        return normalized
            .mapNotNull { preUpdate[it]?.takeIf(ApiIngredient::inInventory) }
            .map { it.copy(inInventory = false, lastModified = now) }
    }

    override suspend fun destroyIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient> {
        val normalized = ingredients.map { it.trim().lowercase() }.distinct()
        val preUpdate = storedIngredients(userId).getAndUpdate { it - normalized.toSet() }
        return normalized.mapNotNull { preUpdate[it] }
    }

    private fun storedIngredients(userId: UserId): MutableStateFlow<Map<String, ApiIngredient>> =
        storage.getOrPut(userId) { MutableStateFlow(emptyMap()) }
}
