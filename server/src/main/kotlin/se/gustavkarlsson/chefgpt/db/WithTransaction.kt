package se.gustavkarlsson.chefgpt.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelSuspendTransaction

// FIXME this gets a singleton database connection. Maybe inject one instead?
suspend fun <T> Database.withTransaction(block: suspend JdbcTransaction.() -> T): T =
    withContext(Dispatchers.IO) {
        inTopLevelSuspendTransaction(this@withTransaction) { block() }
    }
