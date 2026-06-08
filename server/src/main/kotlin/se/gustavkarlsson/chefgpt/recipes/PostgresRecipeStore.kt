package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.util.RepoSyncer
import java.util.UUID
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class PostgresRecipeStore(
    private val db: DatabaseAccess,
) : RecipeStore {
    private val syncer = RepoSyncer<UserId>()

    override suspend fun getRecipes(userId: UserId): List<ApiRecipe> =
        db.use {
            recipeQueries
                .selectByUserId(userId.value.toJavaUuid())
                .executeAsList()
                .map { toApiRecipe(it.id, it.title, it.url, it.image_url) }
        }

    override fun streamRecipes(userId: UserId): Flow<List<ApiRecipe>> =
        syncer
            .notifications(userId)
            .map { getRecipes(userId) }
            .distinctUntilChanged()

    override suspend fun addRecipe(
        userId: UserId,
        title: String,
        url: String,
        imageUrl: String?,
    ): ApiRecipe {
        val added =
            db.use {
                recipeQueries
                    .insert(userId.value.toJavaUuid(), title, url, imageUrl)
                    .executeAsOne()
                    .let { toApiRecipe(it.id, it.title, it.url, it.image_url) }
            }
        syncer.notifyChange(userId)
        return added
    }

    override suspend fun deleteRecipe(
        userId: UserId,
        id: RecipeId,
    ): Boolean {
        val deleted =
            db.use {
                recipeQueries
                    .deleteByUserIdAndId(userId.value.toJavaUuid(), id.value.toJavaUuid())
                    .executeAsList()
                    .isNotEmpty()
            }
        if (deleted) {
            syncer.notifyChange(userId)
        }
        return deleted
    }
}

private fun toApiRecipe(
    id: UUID,
    title: String,
    url: String,
    imageUrl: String?,
): ApiRecipe = ApiRecipe(RecipeId(id.toKotlinUuid()), title, url, imageUrl)
