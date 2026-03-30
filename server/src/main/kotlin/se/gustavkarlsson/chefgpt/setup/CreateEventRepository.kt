package se.gustavkarlsson.chefgpt.setup

import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import se.gustavkarlsson.chefgpt.chats.PostgresEventRepository
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase

fun createEventRepository(database: ChefGptDatabase?): EventRepository =
    if (database != null) {
        PostgresEventRepository(database.eventQueries)
    } else {
        InMemoryEventRepository()
    }
