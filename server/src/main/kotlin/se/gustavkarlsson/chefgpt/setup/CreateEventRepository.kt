package se.gustavkarlsson.chefgpt.setup

import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import se.gustavkarlsson.chefgpt.chats.RethinkDbEventRepository
import se.gustavkarlsson.chefgpt.rethinkdb.RethinkDbAccess

fun createEventRepository(rethinkDb: RethinkDbAccess?): EventRepository =
    if (rethinkDb != null) {
        RethinkDbEventRepository(rethinkDb)
    } else {
        InMemoryEventRepository()
    }
