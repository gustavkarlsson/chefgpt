package se.gustavkarlsson.chefgpt.postgres

import app.cash.sqldelight.driver.r2dbc.R2dbcDriver
import io.r2dbc.spi.ConnectionFactory
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger(PostgresDatabasePool::class.java)

class PostgresDatabasePool(
    private val connectionPool: ConnectionFactory,
) {
    private val borrows = ConcurrentHashMap<DbScope, Entry>()
    private val mutex = Mutex()

    suspend fun <T> use(
        scope: DbScope,
        block: suspend ChefGptDatabase.() -> T,
    ): T {
        val database = borrow(scope)
        return try {
            database.block()
        } finally {
            giveBack(scope)
        }
    }

    private suspend fun borrow(scope: DbScope): ChefGptDatabase =
        mutex.withLock {
            val entry = borrows.getOrPut(scope) { createEntry() }
            val borrowers = entry.incrementBorrowers()
            logger.info("Borrowing database with $scope ($borrowers borrowers)")
            return entry.database
        }

    private suspend fun createEntry(): Entry {
        val connection = connectionPool.create().awaitSingle()
        val driver = R2dbcDriver(connection)
        val database = ChefGptDatabase(driver)
        return Entry(driver, database)
    }

    private suspend fun giveBack(scope: DbScope) {
        mutex.withLock {
            borrows.compute(scope) { _, entry ->
                val entry =
                    checkNotNull(entry) {
                        "No entry when giving back with $scope"
                    }
                val borrowers = entry.decrementBorrowers()
                if (borrowers == 0) {
                    logger.info("Giving back last database with $scope")
                    entry.driver.close()
                    null // Remove it
                } else {
                    logger.info("Giving back database with $scope ($borrowers borrowers)")
                    entry
                }
            }
        }
    }

    private class Entry(
        val driver: R2dbcDriver,
        val database: ChefGptDatabase,
    ) {
        private var borrowers by atomic(0)

        fun incrementBorrowers(): Int = ++borrowers

        fun decrementBorrowers(): Int = --borrowers
    }
}

interface DbScope

data class UserDbScope(
    val userId: UserId,
) : DbScope

data class ChatDbScope(
    val chatId: ChatId,
) : DbScope

data object SingletonDbScope : DbScope // TODO Consider making a no scope/random scope instead

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
