package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.Module
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap

/**
 * A [RecipeRepository] a test can reach directly, for setting up state the HTTP API cannot:
 * modifications are only made by the agent, so there is no route for them.
 *
 * Pass [koinModule] as an extra Koin module to `snapshotTestApplication` to have the
 * server use this repository. The user id is read from the store, since the HTTP API only
 * hands out an opaque session id.
 */
class TestRecipeRepository {
    private val storage = ConcurrentHashMap<UserId, MutableStateFlow<Map<RecipeId, ApiRecipe>>>()
    private val repository = RecipeRepository(InMemoryRecipePersistence(storage))

    val koinModule: Module = module { single<RecipeRepository> { repository } }

    suspend fun modifyRecipe(
        id: RecipeId,
        update: RecipeUpdate,
    ): ApiRecipe {
        val userId = storage.keys.single()
        return checkNotNull(repository.modifyRecipe(userId, id, update)) { "No recipe found with id $id" }
    }
}
