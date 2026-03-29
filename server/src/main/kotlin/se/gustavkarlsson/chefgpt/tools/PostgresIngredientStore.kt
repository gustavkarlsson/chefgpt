package se.gustavkarlsson.chefgpt.tools

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insertIgnore
import org.jetbrains.exposed.v1.r2dbc.selectAll
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.db.withTransaction

private object IngredientTable : UuidTable("ingredient") {
    val userId = uuid("user_id")
    val name = text("name")
}

class PostgresIngredientStore(
    private val db: R2dbcDatabase,
    private val ownerUserId: UserId,
) : IngredientStore {
    override suspend fun getIngredients(): List<String> =
        db.withTransaction {
            IngredientTable
                .selectAll()
                .where { IngredientTable.userId eq ownerUserId.value }
                .map { it[IngredientTable.name] }
                .toList()
        }

    override suspend fun addIngredients(ingredients: List<String>): List<String> =
        db.withTransaction {
            ingredients
                .map { it.trim().lowercase() }
                .filter { ingredient ->
                    IngredientTable
                        .insertIgnore {
                            it[userId] = ownerUserId.value
                            it[name] = ingredient
                        }.insertedCount > 0
                }
        }

    override suspend fun removeIngredients(ingredients: List<String>): List<String> =
        db.withTransaction {
            ingredients.filter { ingredient ->
                IngredientTable.deleteWhere {
                    (userId eq ownerUserId.value) and (name eq ingredient)
                } > 0
            }
        }

    override suspend fun clearIngredients(): List<String> =
        db.withTransaction {
            val removed =
                IngredientTable
                    .selectAll()
                    .where { IngredientTable.userId eq ownerUserId.value }
                    .map { it[IngredientTable.name] }
                    .toList()
            IngredientTable.deleteWhere { userId eq ownerUserId.value }
            removed
        }
}
