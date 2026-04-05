package se.gustavkarlsson.chefgpt.chats

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess
import java.time.OffsetDateTime
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class PostgresChatRepository(
    private val db: PostgresAccess,
) : ChatRepository {
    override suspend fun create(userId: UserId): Chat =
        db.use {
            val row = chatQueries.insert(user_id = userId.value.toJavaUuid()).awaitAsOne()
            Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant())
        }

    override suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        db.use {
            chatQueries
                .deleteByUserIdAndChatId(userId.value.toJavaUuid(), chatId.value.toJavaUuid())
                .awaitAsOneOrNull() != null
        }

    override suspend fun getAll(userId: UserId): List<Chat> =
        db.use {
            chatQueries
                .selectByUserId(userId.value.toJavaUuid())
                .awaitAsList()
                .map { row -> Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant()) }
        }

    override suspend fun get(
        userId: UserId,
        chatId: ChatId,
    ): Chat? =
        db.use {
            chatQueries
                .selectByUserIdAndChatId(userId.value.toJavaUuid(), chatId.value.toJavaUuid())
                .awaitAsOneOrNull()
                ?.let { row -> Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant()) }
        }

    override suspend fun get(chatId: ChatId): Chat? =
        db.use {
            chatQueries
                .selectByChatId(chatId.value.toJavaUuid())
                .awaitAsOneOrNull()
                ?.let { row -> Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant()) }
        }

    override suspend fun contains(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        db.use {
            chatQueries
                .existsByUserIdAndChatId(userId.value.toJavaUuid(), chatId.value.toJavaUuid())
                .awaitAsOne()
        }
}

private fun OffsetDateTime.toKotlinInstant(): Instant = toInstant().toKotlinInstant()
