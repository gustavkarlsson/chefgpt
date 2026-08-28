package se.gustavkarlsson.chefgpt

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
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

object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("duration", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Duration,
    ) {
        encoder.encodeString(value.toIsoString())
    }

    override fun deserialize(decoder: Decoder): Duration = Duration.parseIsoString(decoder.decodeString())
}

@Serializable
private data class IntRangeSurrogate(
    val min: Int,
    val max: Int,
)

object IntRangeSerializer : KSerializer<IntRange> {
    override val descriptor: SerialDescriptor = IntRangeSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: IntRange,
    ) {
        encoder.encodeSerializableValue(IntRangeSurrogate.serializer(), IntRangeSurrogate(value.first, value.last))
    }

    override fun deserialize(decoder: Decoder): IntRange =
        decoder.decodeSerializableValue(IntRangeSurrogate.serializer()).let { it.min..it.max }
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
