package se.gustavkarlsson.chefgpt.db

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

suspend fun <T> R2dbcDatabase.withTransaction(block: suspend R2dbcTransaction.() -> T): T =
    suspendTransaction(this, statement = block)
