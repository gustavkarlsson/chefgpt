package se.gustavkarlsson.chefgpt.chats

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.PostgresDatabasePool
import se.gustavkarlsson.chefgpt.postgres.use
import java.time.OffsetDateTime
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class PostgresChatRepository(
    private val dbPool: PostgresDatabasePool,
) : ChatRepository {
    override suspend fun create(userId: UserId): Chat =
        dbPool.use(userId) {
            val row = chatQueries.insert(user_id = userId.value.toJavaUuid()).awaitAsOne()
            Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant())
        }

    override suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        dbPool.use(userId) {
            chatQueries
                .deleteByUserIdAndChatId(user_id = userId.value.toJavaUuid(), id = chatId.value.toJavaUuid())
                .awaitAsOneOrNull() != null
        }

    override suspend fun getAll(userId: UserId): List<Chat> =
        dbPool.use(userId) {
            chatQueries
                .selectByUserId(user_id = userId.value.toJavaUuid())
                .awaitAsList()
                .map { row -> Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant()) }
        }

    override suspend fun get(
        userId: UserId,
        chatId: ChatId,
    ): Chat? =
        dbPool.use(userId) {
            chatQueries
                .selectByUserIdAndChatId(user_id = userId.value.toJavaUuid(), id = chatId.value.toJavaUuid())
                .awaitAsOneOrNull()
                ?.let { row -> Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant()) }
        }

    override suspend fun contains(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        dbPool.use(userId) {
            chatQueries
                .existsByUserIdAndChatId(user_id = userId.value.toJavaUuid(), id = chatId.value.toJavaUuid())
                .awaitAsOne()
        }
}

private fun OffsetDateTime.toKotlinInstant(): Instant = toInstant().toKotlinInstant()
