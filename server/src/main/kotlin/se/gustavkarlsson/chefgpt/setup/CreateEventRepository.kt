package se.gustavkarlsson.chefgpt.setup

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import se.gustavkarlsson.chefgpt.chats.PostgresEventRepository

fun createEventRepository(database: R2dbcDatabase?): EventRepository =
    if (database != null) {
        PostgresEventRepository(database)
    } else {
        InMemoryEventRepository()
    }
