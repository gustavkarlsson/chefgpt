package se.gustavkarlsson.chefgpt.ingredients

import org.jetbrains.annotations.VisibleForTesting
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryIngredientStoreFactory {
    private val storage = ConcurrentHashMap<UserId, CopyOnWriteArrayList<String>>()

    fun create(userId: UserId): IngredientStore =
        InMemoryIngredientStore(storage.computeIfAbsent(userId) { CopyOnWriteArrayList() })
}

@VisibleForTesting
class InMemoryIngredientStore(
    private val ingredients: MutableList<String>,
) : IngredientStore {
    override suspend fun getIngredients(): List<String> = ingredients.toList()

    override suspend fun addIngredients(ingredients: List<String>): List<String> {
        val normalized = ingredients.map { it.trim().lowercase() }
        val added = normalized.filter { it !in this.ingredients }
        this.ingredients += added
        return added
    }

    override suspend fun removeIngredients(ingredients: List<String>): List<String> =
        ingredients.filter { this.ingredients.remove(it) }

    override suspend fun clearIngredients(): List<String> {
        val removed = ingredients.toList()
        ingredients.clear()
        return removed
    }
}
