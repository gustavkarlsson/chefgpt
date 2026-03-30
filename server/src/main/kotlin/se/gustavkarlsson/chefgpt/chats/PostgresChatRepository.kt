package se.gustavkarlsson.chefgpt.chats

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.db.ChatQueries
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class PostgresChatRepository(
    private val chatQueries: ChatQueries,
) : ChatRepository {
    override suspend fun create(userId: UserId): Chat {
        val id = chatQueries.insert(user_id = userId.value.toJavaUuid()).awaitAsOne()
        return Chat(ChatId(id.toKotlinUuid()))
    }

    override suspend fun delete(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        chatQueries
            .deleteByUserIdAndChatId(userId.value.toJavaUuid(), chatId.value.toJavaUuid())
            .awaitAsOneOrNull() != null

    override suspend fun getAll(userId: UserId): List<Chat> =
        chatQueries
            .selectByUserId(userId.value.toJavaUuid())
            .awaitAsList()
            .map { row -> Chat(ChatId(row.id.toKotlinUuid())) }

    override suspend fun get(
        userId: UserId,
        chatId: ChatId,
    ): Chat? =
        chatQueries
            .selectByUserIdAndChatId(userId.value.toJavaUuid(), chatId.value.toJavaUuid())
            .awaitAsOneOrNull()
            ?.let { row -> Chat(ChatId(row.id.toKotlinUuid())) }

    override suspend fun get(chatId: ChatId): Chat? =
        chatQueries
            .selectByChatId(chatId.value.toJavaUuid())
            .awaitAsOneOrNull()
            ?.let { row -> Chat(ChatId(row.id.toKotlinUuid())) }

    override suspend fun contains(
        userId: UserId,
        chatId: ChatId,
    ): Boolean =
        chatQueries
            .existsByUserIdAndChatId(userId.value.toJavaUuid(), chatId.value.toJavaUuid())
            .awaitAsOne()
}
