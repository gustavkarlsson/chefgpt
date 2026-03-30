package se.gustavkarlsson.chefgpt.auth

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.UuidValueSerializer
import kotlin.uuid.Uuid

@Serializable(with = UserIdSerializer::class)
@JvmInline
value class UserId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): UserId = UserId(Uuid.random())

        fun parse(uuidString: String): UserId = UserId(Uuid.parse(uuidString))

        fun parseOrNull(uuidString: String): UserId? = Uuid.parseOrNull(uuidString)?.let(::UserId)
    }
}

object UserIdSerializer : UuidValueSerializer<UserId>("user-id", ::UserId, UserId::value)
