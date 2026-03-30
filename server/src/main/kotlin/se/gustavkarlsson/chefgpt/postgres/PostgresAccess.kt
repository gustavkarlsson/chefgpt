package se.gustavkarlsson.chefgpt.postgres

import app.cash.sqldelight.driver.r2dbc.R2dbcDriver
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitSingle
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase

class PostgresAccess(
    private val connectionFactory: ConnectionFactory,
) {
    /**
     * Use the database. A connection opened for the duration of the block.
     */
    suspend fun <T> use(block: suspend ChefGptDatabase.() -> T): T {
        val driver = createDriver()
        return driver.use { driver ->
            ChefGptDatabase(driver).block()
        }
    }

    /**
     * Produce a stream from the database. A connection opened for the duration of the collection.
     */
    fun <T> stream(block: suspend ChefGptDatabase.() -> Flow<T>): Flow<T> =
        flow {
            createDriver().use { driver ->
                val flow = ChefGptDatabase(driver).block()
                emitAll(flow)
            }
        }

    private suspend fun createDriver(): R2dbcDriver {
        val connection = connectionFactory.create().awaitSingle()
        return R2dbcDriver(connection)
    }
}
