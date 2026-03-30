package se.gustavkarlsson.chefgpt.tools

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.db.IngredientQueries
import kotlin.uuid.toJavaUuid

class PostgresIngredientStore(
    private val ingredientQueries: IngredientQueries,
    private val ownerUserId: UserId,
) : IngredientStore {
    override suspend fun getIngredients(): List<String> =
        ingredientQueries
            .selectByUserId(ownerUserId.value.toJavaUuid())
            .awaitAsList()
            .map { it.name }

    override suspend fun addIngredients(ingredients: List<String>): List<String> =
        ingredientQueries.transactionWithResult {
            ingredients
                .map { it.trim().lowercase() }
                .mapNotNull { ingredient ->
                    ingredientQueries
                        .insert(
                            user_id = ownerUserId.value.toJavaUuid(),
                            name = ingredient,
                        ).awaitAsOneOrNull()
                }
        }

    override suspend fun removeIngredients(ingredients: List<String>): List<String> =
        ingredientQueries.transactionWithResult {
            ingredients.mapNotNull { ingredient ->
                ingredientQueries
                    .deleteByUserIdAndName(ownerUserId.value.toJavaUuid(), ingredient)
                    .awaitAsOneOrNull()
            }
        }

    override suspend fun clearIngredients(): List<String> =
        ingredientQueries
            .deleteByUserId(ownerUserId.value.toJavaUuid())
            .awaitAsList()
}
