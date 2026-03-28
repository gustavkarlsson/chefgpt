package se.gustavkarlsson.chefgpt.auth

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.uuid.Uuid

@Serializable(with = UserIdSerializer::class)
@JvmInline
value class UserId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): UserId = UserId(Uuid.random())

        fun parse(uuidString: String): UserId = UserId(Uuid.parse(uuidString))
    }
}

object UserIdSerializer : KSerializer<UserId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UserId", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: UserId,
    ) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): UserId = UserId(Uuid.parse(decoder.decodeString()))
}
