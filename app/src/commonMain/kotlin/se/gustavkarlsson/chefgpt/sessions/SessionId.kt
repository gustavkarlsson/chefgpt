package se.gustavkarlsson.chefgpt.sessions

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

@Serializable(with = SessionIdSerializer::class)
@JvmInline
value class SessionId(
    val value: String,
) {
    override fun toString(): String = value
}

object SessionIdSerializer : KSerializer<SessionId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("session-id", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: SessionId,
    ) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): SessionId = SessionId(decoder.decodeString())
}
