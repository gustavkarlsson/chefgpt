package se.gustavkarlsson.chefgpt

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.uuid.Uuid

abstract class UuidValueSerializer<T>(
    serialName: String,
    private val wrap: (Uuid) -> T,
    private val unwrap: (T) -> Uuid,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) {
        encoder.encodeString(unwrap(value).toString())
    }

    override fun deserialize(decoder: Decoder): T = wrap(Uuid.parse(decoder.decodeString()))
}

abstract class StringValueSerializer<T>(
    serialName: String,
    private val wrap: (String) -> T,
    private val unwrap: (T) -> String,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) {
        encoder.encodeString(unwrap(value))
    }

    override fun deserialize(decoder: Decoder): T = wrap(decoder.decodeString())
}
