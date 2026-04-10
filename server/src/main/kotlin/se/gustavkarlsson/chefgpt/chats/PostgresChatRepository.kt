package se.gustavkarlsson.chefgpt.chats

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.util.RepoSyncer
import java.time.OffsetDateTime
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class PostgresChatRepository(
    private val db: DatabaseAccess,
) : ChatRepository {
    private val syncer = RepoSyncer<UserId>()

    override suspend fun create(userId: UserId): Chat {
        val chat =
            db.use {
                val row = chatQueries.insert(user_id = userId.value.toJavaUuid()).executeAsOne()
                Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant())
            }
        syncer.notifyChange(userId)
        return chat
    }

    override suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean {
        val deleted =
            db.use {
                chatQueries
                    .deleteByUserIdAndChatId(user_id = userId.value.toJavaUuid(), id = chatId.value.toJavaUuid())
                    .executeAsOneOrNull() != null
            }
        if (deleted) {
            syncer.notifyChange(userId)
        }
        return deleted
    }

    override fun stream(userId: UserId): Flow<List<Chat>> =
        syncer
            .notifications(userId)
            .map {
                db.use {
                    chatQueries
                        .selectByUserId(user_id = userId.value.toJavaUuid())
                        .executeAsList()
                        .map { row -> Chat(ChatId(row.id.toKotlinUuid()), row.created_at.toKotlinInstant()) }
                }
            }.distinctUntilChanged()

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
