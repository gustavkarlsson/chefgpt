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

@Serializable(with = SessionIdSerializer::class)
@JvmInline
value class SessionId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): SessionId = SessionId(Uuid.random())

        fun parse(uuidString: String): SessionId = SessionId(Uuid.parse(uuidString))

        fun parseOrNull(uuidString: String): SessionId? = Uuid.parseOrNull(uuidString)?.let(::SessionId)
    }
}

object SessionIdSerializer : KSerializer<SessionId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SessionId", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: SessionId,
    ) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): SessionId = SessionId(Uuid.parse(decoder.decodeString()))
}
