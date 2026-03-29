package se.gustavkarlsson.chefgpt

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

// FIXME Remove this, or move it into the app project. It's not used by the server (which handles session ID:s behind the scenes)
@Serializable(with = SessionIdSerializer::class)
@JvmInline
value class SessionId(
    val value: String,
) {
    override fun toString(): String = value
}

object SessionIdSerializer : KSerializer<SessionId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SessionId", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: SessionId,
    ) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): SessionId = SessionId(decoder.decodeString())
}
