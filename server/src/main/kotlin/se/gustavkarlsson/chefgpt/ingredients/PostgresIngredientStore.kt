package se.gustavkarlsson.chefgpt.ingredients

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.PostgresDatabasePool
import se.gustavkarlsson.chefgpt.postgres.use
import kotlin.uuid.toJavaUuid

class PostgresIngredientStore(
    private val dbPool: PostgresDatabasePool,
) : IngredientStore {
    override suspend fun getIngredients(userId: UserId): List<String> =
        dbPool.use(userId) {
            ingredientQueries
                .selectByUserId(userId.value.toJavaUuid())
                .awaitAsList()
                .map { it.name }
        }

    override fun streamIngredients(userId: UserId): Flow<List<String>> =
        channelFlow {
            dbPool.use(userId) {
                ingredientQueries
                    .selectByUserId(userId.value.toJavaUuid())
                    .asFlow()
                    .map { it.awaitAsList() }
                    .map { ingredients -> ingredients.map { it.name } }
                    .collect { send(it) }
            }
        }

    override suspend fun addIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<String> =
        dbPool.use(userId) {
            ingredientQueries.transactionWithResult {
                ingredients
                    .map { it.trim().lowercase() }
                    .mapNotNull { ingredient ->
                        ingredientQueries
                            .insert(
                                user_id = userId.value.toJavaUuid(),
                                name = ingredient,
                            ).awaitAsOneOrNull()
                    }
            }
        }

    override suspend fun removeIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<String> =
        dbPool.use(userId) {
            ingredientQueries.transactionWithResult {
                ingredients.mapNotNull { ingredient ->
                    ingredientQueries
                        .deleteByUserIdAndName(userId.value.toJavaUuid(), ingredient)
                        .awaitAsOneOrNull()
                }
            }
        }

    override suspend fun clearIngredients(userId: UserId): List<String> =
        dbPool.use(userId) {
            ingredientQueries
                .deleteByUserId(userId.value.toJavaUuid())
                .awaitAsList()
        }
}
