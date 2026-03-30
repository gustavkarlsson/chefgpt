package se.gustavkarlsson.chefgpt.rethinkdb

import com.rethinkdb.RethinkDB
import com.rethinkdb.gen.exc.ReqlOpFailedError
import com.rethinkdb.net.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(RethinkDbAccess::class.java)

class RethinkDbAccess(
    private val host: String,
    private val port: Int,
    private val database: String,
    private val username: String? = null,
    private val password: String? = null,
) {
    val r: RethinkDB = RethinkDB.r

    fun createConnection(): Connection =
        r
            .connection()
            .hostname(host)
            .port(port)
            .db(database)
            .let { builder ->
                if (username != null && password != null) {
                    builder.user(username, password)
                } else {
                    builder
                }
            }.connect()

    suspend fun <T> use(block: (RethinkDB, Connection) -> T): T =
        withContext(Dispatchers.IO) {
            createConnection().use { conn ->
                block(r, conn)
            }
        }

    fun initialize() {
        createConnection().use { conn ->
            try {
                r.dbCreate(database).run(conn)
                logger.info("Created database '{}'", database)
            } catch (_: ReqlOpFailedError) {
                logger.debug("Database '{}' already exists", database)
            }

            try {
                r.db(database).tableCreate("events").run(conn)
                logger.info("Created table 'events'")
            } catch (_: ReqlOpFailedError) {
                logger.debug("Table 'events' already exists")
            }

            try {
                r
                    .db(database)
                    .table("events")
                    .indexCreate("chat_id")
                    .run(conn)
                logger.info("Created index 'chat_id' on 'events'")
            } catch (_: ReqlOpFailedError) {
                logger.debug("Index 'chat_id' already exists")
            }

            r
                .db(database)
                .table("events")
                .indexWait("chat_id")
                .run(conn)
        }
    }
}
