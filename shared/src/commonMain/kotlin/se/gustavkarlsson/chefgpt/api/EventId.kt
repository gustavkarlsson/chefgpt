package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable(with = EventIdSerializer::class)
@JvmInline
value class EventId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): EventId = EventId(Uuid.random())
    }
}

object EventIdSerializer : UuidValueSerializer<EventId>("event-id", ::EventId, EventId::value)
