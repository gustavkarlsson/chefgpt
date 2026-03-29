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

object JoinIdSerializer : KSerializer<JoinId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("JoinId", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: JoinId,
    ) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): JoinId = JoinId(Uuid.parse(decoder.decodeString()))
}
