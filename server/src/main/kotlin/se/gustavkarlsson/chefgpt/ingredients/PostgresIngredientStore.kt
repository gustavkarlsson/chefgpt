package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.util.RepoSyncer
import kotlin.uuid.toJavaUuid

class PostgresIngredientStore(
    private val db: DatabaseAccess,
) : IngredientStore {
    private val syncer = RepoSyncer<UserId>()

    override suspend fun getIngredients(userId: UserId): List<String> =
        db.use {
            ingredientQueries
                .selectByUserId(userId.value.toJavaUuid())
                .executeAsList()
                .map { it.name }
        }

    override fun streamIngredients(userId: UserId): Flow<List<String>> =
        syncer
            .notifications(userId)
            .map {
                db.use {
                    ingredientQueries
                        .selectByUserId(userId.value.toJavaUuid())
                        .executeAsList()
                        .map { it.name }
                }
            }.distinctUntilChanged()

    override suspend fun addIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<String> {
        val added =
            db.use {
                ingredientQueries.transactionWithResult {
                    ingredients
                        .map { it.trim().lowercase() }
                        .mapNotNull { ingredient ->
                            ingredientQueries
                                .insert(
                                    user_id = userId.value.toJavaUuid(),
                                    name = ingredient,
                                ).executeAsOneOrNull()
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
    ): List<String> {
        val removed =
            db.use {
                ingredientQueries.transactionWithResult {
                    ingredients.mapNotNull { ingredient ->
                        ingredientQueries
                            .deleteByUserIdAndName(userId.value.toJavaUuid(), ingredient)
                            .executeAsOneOrNull()
                    }
                }
            }
        if (removed.isNotEmpty()) {
            syncer.notifyChange(userId)
        }
        return removed
    }

    override suspend fun clearIngredients(userId: UserId): List<String> {
        val removed =
            db.use {
                ingredientQueries
                    .deleteByUserId(userId.value.toJavaUuid())
                    .executeAsList()
            }
        if (removed.isNotEmpty()) {
            syncer.notifyChange(userId)
        }
        return removed
    }
}
