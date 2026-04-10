package se.gustavkarlsson.chefgpt.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase

class DatabaseAccess(
    private val database: ChefGptDatabase,
) {
    suspend fun <T> use(block: suspend ChefGptDatabase.() -> T): T = withContext(Dispatchers.IO) { database.block() }
}
