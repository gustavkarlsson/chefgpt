package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.UuidValueSerializer
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable(with = JoinIdSerializer::class)
@JvmInline
value class JoinId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): JoinId = JoinId(Uuid.random())

        fun parse(uuidString: String): JoinId = JoinId(Uuid.parse(uuidString))

        fun parseOrNull(uuidString: String): JoinId? = Uuid.parseOrNull(uuidString)?.let(::JoinId)
    }
}

object JoinIdSerializer : UuidValueSerializer<JoinId>("join-id", ::JoinId, JoinId::value)
