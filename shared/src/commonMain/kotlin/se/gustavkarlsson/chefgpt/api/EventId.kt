package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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

object EventIdSerializer : KSerializer<EventId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EventId", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: EventId,
    ) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): EventId = EventId(Uuid.parse(decoder.decodeString()))
}
