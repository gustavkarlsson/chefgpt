package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.util.RepoSyncer
import java.time.OffsetDateTime
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.toJavaUuid

class PostgresIngredientStore(
    private val db: DatabaseAccess,
) : IngredientStore {
    private val syncer = RepoSyncer<UserId>()

    override suspend fun getIngredients(userId: UserId): List<ApiIngredient> =
        db.use {
            ingredientQueries
                .selectByUserId(userId.value.toJavaUuid())
                .executeAsList()
                .map { ApiIngredient(it.name, it.last_modified.toKotlinInstant(), it.in_inventory) }
        }

    override fun streamIngredients(userId: UserId): Flow<List<ApiIngredient>> =
        syncer
            .notifications(userId)
            .map {
                db.use {
                    ingredientQueries
                        .selectByUserId(userId.value.toJavaUuid())
                        .executeAsList()
                        .map { ApiIngredient(it.name, it.last_modified.toKotlinInstant(), it.in_inventory) }
                }
            }.distinctUntilChanged()

    override suspend fun addIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient> {
        val added =
            db.use {
                ingredientQueries.transactionWithResult {
                    ingredients
                        .map { it.trim().lowercase() }
                        .distinct()
                        .mapNotNull { ingredient ->
                            val existing =
                                ingredientQueries
                                    .selectByUserIdAndName(userId.value.toJavaUuid(), ingredient)
                                    .executeAsOneOrNull()
                            if (existing?.in_inventory == true) {
                                null
                            } else {
                                ingredientQueries
                                    .upsert(userId.value.toJavaUuid(), ingredient)
                                    .executeAsOne()
                                    .let { ApiIngredient(it.name, it.last_modified.toKotlinInstant(), it.in_inventory) }
                            }
                        }
                }
            }
        if (added.isNotEmpty()) {
            syncer.notifyChange(userId)
        }
        return added
    }

    override suspend fun removeIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient> {
        val removed =
            db.use {
                ingredientQueries.transactionWithResult {
                    ingredients
                        .map { it.trim().lowercase() }
                        .mapNotNull { ingredient ->
                            ingredientQueries
                                .softRemoveByUserIdAndName(userId.value.toJavaUuid(), ingredient)
                                .executeAsOneOrNull()
                                ?.let { ApiIngredient(it.name, it.last_modified.toKotlinInstant(), it.in_inventory) }
                        }
                }
            }
        if (removed.isNotEmpty()) {
            syncer.notifyChange(userId)
        }
        return removed
    }

    override suspend fun destroyIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient> {
        val destroyed =
            db.use {
                ingredientQueries.transactionWithResult {
                    ingredients
                        .map { it.trim().lowercase() }
                        .mapNotNull { ingredient ->
                            ingredientQueries
                                .destroyByUserIdAndName(userId.value.toJavaUuid(), ingredient)
                                .executeAsOneOrNull()
                                ?.let { ApiIngredient(it.name, it.last_modified.toKotlinInstant(), it.in_inventory) }
                        }
                }
            }
        if (destroyed.isNotEmpty()) {
            syncer.notifyChange(userId)
        }
        return destroyed
    }
}

private fun OffsetDateTime.toKotlinInstant(): Instant = toInstant().toKotlinInstant()
