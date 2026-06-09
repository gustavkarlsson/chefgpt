package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeSummaryId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.util.RepoSyncer
import java.util.UUID
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class PostgresRecipeSummaryStore(
    private val db: DatabaseAccess,
) : RecipeSummaryStore {
    private val syncer = RepoSyncer<UserId>()

    override suspend fun getRecipeSummaries(userId: UserId): List<ApiRecipeSummary> =
        db.use {
            recipeSummaryQueries
                .selectByUserId(userId.value.toJavaUuid())
                .executeAsList()
                .map { toApiRecipeSummary(it.id, it.title, it.spoonacular_id, it.image_url) }
        }

    override suspend fun getRecipeSummary(
        userId: UserId,
        id: RecipeSummaryId,
    ): ApiRecipeSummary? =
        db.use {
            recipeSummaryQueries
                .selectByUserIdAndId(userId.value.toJavaUuid(), id.value.toJavaUuid())
                .executeAsOneOrNull()
                ?.let { toApiRecipeSummary(it.id, it.title, it.spoonacular_id, it.image_url) }
        }

    override fun streamRecipeSummaries(userId: UserId): Flow<List<ApiRecipeSummary>> =
        syncer
            .notifications(userId)
            .map { getRecipeSummaries(userId) }
            .distinctUntilChanged()

    override suspend fun addRecipeSummary(
        userId: UserId,
        title: String,
        spoonacularId: SpoonacularId,
        imageUrl: String?,
    ): ApiRecipeSummary {
        val added =
            db.use {
                recipeSummaryQueries
                    .insert(userId.value.toJavaUuid(), title, spoonacularId.value, imageUrl)
                    .executeAsOne()
                    .let { toApiRecipeSummary(it.id, it.title, it.spoonacular_id, it.image_url) }
            }
        syncer.notifyChange(userId)
        return added
    }

    override suspend fun deleteRecipeSummary(
        userId: UserId,
        id: RecipeSummaryId,
    ): Boolean {
        val deleted =
            db.use {
                recipeSummaryQueries
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

private fun toApiRecipeSummary(
    id: UUID,
    title: String,
    spoonacularId: Long,
    imageUrl: String?,
): ApiRecipeSummary =
    ApiRecipeSummary(RecipeSummaryId(id.toKotlinUuid()), title, SpoonacularId(spoonacularId), imageUrl)
