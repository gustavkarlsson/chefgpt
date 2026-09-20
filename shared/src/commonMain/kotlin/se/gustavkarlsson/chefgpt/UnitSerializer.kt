package se.gustavkarlsson.chefgpt

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * Unit has no public serializer in kotlinx.serialization.
 * Serializes as an empty object, matching the built-in one.
 */
object UnitSerializer : KSerializer<Unit> {
    override val descriptor = buildClassSerialDescriptor("kotlin.Unit")

    override fun serialize(
        encoder: Encoder,
        value: Unit,
    ) {
        encoder.encodeStructure(descriptor) {}
    }

    override fun deserialize(decoder: Decoder) {
        decoder.decodeStructure(descriptor) {}
        return Unit
    }
}
