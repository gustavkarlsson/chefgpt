package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import java.time.OffsetDateTime
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class PostgresChatRepository(
    private val db: DatabaseAccess,
) : ChatRepository {
    override suspend fun create(userId: UserId): Chat =
        db.use {
            val row = chatQueries.insert(user_id = userId.value.toJavaUuid()).executeAsOne()
            Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant())
        }

    override suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        db.use {
            chatQueries
                .deleteByUserIdAndChatId(user_id = userId.value.toJavaUuid(), id = chatId.value.toJavaUuid())
                .executeAsOneOrNull() != null
        }

    override suspend fun getAll(userId: UserId): List<Chat> =
        db.use {
            chatQueries
                .selectByUserId(user_id = userId.value.toJavaUuid())
                .executeAsList()
                .map { row -> Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant()) }
        }

    override suspend fun get(
        userId: UserId,
        chatId: ChatId,
    ): Chat? =
        db.use {
            chatQueries
                .selectByUserIdAndChatId(user_id = userId.value.toJavaUuid(), id = chatId.value.toJavaUuid())
                .executeAsOneOrNull()
                ?.let { row -> Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant()) }
        }

    override suspend fun contains(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        db.use {
            chatQueries
                .existsByUserIdAndChatId(user_id = userId.value.toJavaUuid(), id = chatId.value.toJavaUuid())
                .executeAsOne()
        }
}

private fun OffsetDateTime.toKotlinInstant(): Instant = toInstant().toKotlinInstant()
