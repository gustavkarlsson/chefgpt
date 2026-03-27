package se.gustavkarlsson.chefgpt.tools

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import se.gustavkarlsson.chefgpt.db.withTransaction

private object IngredientTable : UuidTable("ingredient") {
    val name = text("name")
}

class PostgresIngredientStore(
    private val db: Database,
) : IngredientStore {
    override suspend fun getIngredients(): List<String> =
        db.withTransaction {
            IngredientTable.selectAll().map { it[IngredientTable.name] }
        }

    override suspend fun addIngredients(ingredients: List<String>): List<String> =
        db.withTransaction {
            ingredients
                .map { it.trim().lowercase() }
                .filter { ingredient ->
                    IngredientTable.insertIgnore { it[name] = ingredient }.insertedCount > 0
                }
        }

    override suspend fun removeIngredients(ingredients: List<String>): List<String> =
        db.withTransaction {
            ingredients.filter { ingredient ->
                IngredientTable.deleteWhere { name eq ingredient } > 0
            }
        }

    override suspend fun clearIngredients(): List<String> =
        db.withTransaction {
            val removed = IngredientTable.selectAll().map { it[IngredientTable.name] }
            IngredientTable.deleteAll()
            removed
        }
}
