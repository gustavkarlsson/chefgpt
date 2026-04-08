package se.gustavkarlsson.chefgpt.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase

class PostgresDatabasePool(
    private val database: ChefGptDatabase,
) {
    suspend fun <T> use(
        scope: DbScope,
        block: suspend ChefGptDatabase.() -> T,
    ): T = withContext(Dispatchers.IO) { database.block() }
}

interface DbScope

data class UserDbScope(
    val userId: UserId,
) : DbScope

data class ChatDbScope(
    val chatId: ChatId,
) : DbScope

data object SingletonDbScope : DbScope

suspend fun <T> PostgresDatabasePool.use(
    scope: UserId,
    block: suspend ChefGptDatabase.() -> T,
): T = use(UserDbScope(scope), block)

suspend fun <T> PostgresDatabasePool.use(
    scope: ChatId,
    block: suspend ChefGptDatabase.() -> T,
): T = use(ChatDbScope(scope), block)

suspend fun <T> PostgresDatabasePool.useSingletonScope(block: suspend ChefGptDatabase.() -> T): T =
    use(SingletonDbScope, block)
