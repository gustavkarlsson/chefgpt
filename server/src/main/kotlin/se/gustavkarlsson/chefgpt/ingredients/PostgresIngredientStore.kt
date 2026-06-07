package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.util.RepoSyncer
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class PostgresIngredientStore(
    private val db: DatabaseAccess,
) : IngredientStore {
    private val syncer = RepoSyncer<UserId>()

    override suspend fun getIngredients(userId: UserId): List<ApiIngredient> =
        db.use {
            ingredientQueries
                .selectByUserId(userId.value.toJavaUuid())
                .executeAsList()
                .map { toApiIngredient(it.id, it.name, it.last_modified, it.in_inventory) }
        }

    override fun streamIngredients(userId: UserId): Flow<List<ApiIngredient>> =
        syncer
            .notifications(userId)
            .map {
                db.use {
                    ingredientQueries
                        .selectByUserId(userId.value.toJavaUuid())
                        .executeAsList()
                        .map { toApiIngredient(it.id, it.name, it.last_modified, it.in_inventory) }
                }
            }.distinctUntilChanged()

    override suspend fun createIngredients(
        userId: UserId,
        ingredients: List<String>,
    ): List<ApiIngredient> {
        val names = ingredients.map { it.trim().lowercase() }.distinct()
        if (names.isEmpty()) return emptyList()
        val added =
            db.use {
                ingredientQueries
                    .create(userId.value.toJavaUuid(), names.toTypedArray())
                    .executeAsList()
                    .map { toApiIngredient(it.id, it.name, it.last_modified, it.in_inventory) }
            }
        if (added.isNotEmpty()) {
            syncer.notifyChange(userId)
        }
        return added
    }

    override suspend fun setInventory(
        userId: UserId,
        ids: List<IngredientId>,
        inInventory: Boolean,
    ): List<ApiIngredient> {
        if (ids.isEmpty()) return emptyList()
        val updated =
            db.use {
                ingredientQueries
                    .setInventoryByUserIdAndIds(inInventory, userId.value.toJavaUuid(), ids.toTextArray())
                    .executeAsList()
                    .map { toApiIngredient(it.id, it.name, it.last_modified, it.in_inventory) }
            }
        if (updated.isNotEmpty()) {
            syncer.notifyChange(userId)
        }
        return updated
    }

    override suspend fun destroyIngredients(
        userId: UserId,
        ids: List<IngredientId>,
    ): List<ApiIngredient> {
        if (ids.isEmpty()) return emptyList()
        val destroyed =
            db.use {
                ingredientQueries
                    .destroyByUserIdAndIds(userId.value.toJavaUuid(), ids.toTextArray())
                    .executeAsList()
                    .map { toApiIngredient(it.id, it.name, it.last_modified, it.in_inventory) }
            }
        if (destroyed.isNotEmpty()) {
            syncer.notifyChange(userId)
        }
        return destroyed
    }
}

private fun toApiIngredient(
    id: UUID,
    name: String,
    lastModified: OffsetDateTime,
    inInventory: Boolean,
): ApiIngredient = ApiIngredient(IngredientId(id.toKotlinUuid()), name, lastModified.toKotlinInstant(), inInventory)

private fun OffsetDateTime.toKotlinInstant(): Instant = toInstant().toKotlinInstant()

// pgjdbc has no array encoder for UUID, so ids are passed as text and cast to uuid[] in SQL.
private fun List<IngredientId>.toTextArray(): Array<String> = map { it.value.toString() }.toTypedArray()
