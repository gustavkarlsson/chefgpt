package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.IngredientId
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

    override suspend fun createIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient> {
        val now = Clock.System.now()
        val normalized = ingredients.map { it.trim().lowercase() }.distinct()
        var addedNames: List<String> = emptyList()
        val updated =
            storedIngredients(userId).updateAndGet { current ->
                addedNames = normalized.filter { current[it]?.inInventory != true }
                current +
                    addedNames.associateWith { name ->
                        ApiIngredient(
                            id = current[name]?.id ?: IngredientId.random(),
                            name = name,
                            lastModified = now,
                            inInventory = true,
                        )
                    }
            }
        return addedNames.map { updated.getValue(it) }
    }

    override suspend fun setInventory(
        userId: UserId,
        ids: List<IngredientId>,
        inInventory: Boolean,
    ): List<ApiIngredient> {
        val now = Clock.System.now()
        val idSet = ids.toSet()
        val updated =
            storedIngredients(userId).updateAndGet { current ->
                current +
                    current.values
                        .filter { it.id in idSet }
                        .associate { it.name to it.copy(inInventory = inInventory, lastModified = now) }
            }
        return updated.values.filter { it.id in idSet }
    }

    override suspend fun destroyIngredients(
        userId: UserId,
        ids: List<IngredientId>,
    ): List<ApiIngredient> {
        val idSet = ids.toSet()
        val preUpdate =
            storedIngredients(userId).getAndUpdate {
                it.filterValues { ingredient ->
                    ingredient.id !in idSet
                }
            }
        return preUpdate.values.filter { it.id in idSet }
    }

    private fun storedIngredients(userId: UserId): MutableStateFlow<Map<String, ApiIngredient>> =
        storage.getOrPut(userId) { MutableStateFlow(emptyMap()) }
}
