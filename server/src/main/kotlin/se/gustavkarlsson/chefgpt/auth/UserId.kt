package se.gustavkarlsson.chefgpt.auth

import kotlin.uuid.Uuid

@JvmInline
value class UserId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): UserId = UserId(Uuid.random())

        fun parse(uuidString: String): UserId = UserId(Uuid.parse(uuidString))
    }
}
