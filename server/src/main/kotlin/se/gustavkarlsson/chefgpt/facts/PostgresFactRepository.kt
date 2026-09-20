package se.gustavkarlsson.chefgpt.facts

import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import kotlin.uuid.toJavaUuid

class PostgresFactRepository(
    private val db: DatabaseAccess,
    private val json: Json,
) : FactRepository {
    override suspend fun getFacts(userId: UserId): UserFacts =
        db
            .use {
                factQueries
                    .selectByUserId(userId.value.toJavaUuid())
                    .executeAsOneOrNull()
            }?.let { json.decodeFromString<UserFacts>(it) } // TODO Consider more SQL-structured facts
            ?: UserFacts.UNKNOWN

    override suspend fun updateFacts(
        userId: UserId,
        update: UserFactsUpdate,
    ): UserFacts {
        val updated = getFacts(userId).applyUpdate(update)
        db.use {
            factQueries.upsert(userId.value.toJavaUuid(), json.encodeToString(UserFacts.serializer(), updated))
        }
        return updated
    }
}
