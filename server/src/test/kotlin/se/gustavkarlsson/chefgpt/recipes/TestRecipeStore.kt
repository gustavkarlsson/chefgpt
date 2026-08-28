package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap

/**
 * An [InMemoryRecipeStore] a test can reach directly, for setting up state the HTTP API
 * cannot: modifications are only made by the agent, so there is no route for them.
 *
 * Pass [koinModule] as an extra Koin module to `snapshotTestApplication` to have the
 * server use this store. The user id is read from the store, since the HTTP API only
 * hands out an opaque session id.
 */
class TestRecipeStore {
    private val storage = ConcurrentHashMap<UserId, MutableStateFlow<Map<RecipeId, ApiRecipe>>>()
    private val store = InMemoryRecipeStore(storage)

    val koinModule: Module = module { single { store } bind RecipeStore::class }

    suspend fun modifyRecipe(
        id: RecipeId,
        update: RecipeUpdate,
    ): ApiRecipe {
        val userId = storage.keys.single()
        return checkNotNull(store.modifyRecipe(userId, id, update)) { "No recipe found with id $id" }
    }
}
