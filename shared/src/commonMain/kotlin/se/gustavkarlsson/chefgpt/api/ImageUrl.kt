package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

@Serializable(with = ImageUrlSerializer::class)
@JvmInline
value class ImageUrl(
    val value: String,
) {
    override fun toString(): String = value
}

object ImageUrlSerializer : KSerializer<ImageUrl> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ImageUrl", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ImageUrl,
    ) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ImageUrl = ImageUrl(decoder.decodeString())
}
