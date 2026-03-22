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

@Serializable(with = ChatIdSerializer::class)
@JvmInline
value class ChatId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): ChatId = ChatId(Uuid.random())

        fun parse(uuidString: String): ChatId = ChatId(Uuid.parse(uuidString))
    }
}

object ChatIdSerializer : KSerializer<ChatId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ChatId", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ChatId,
    ) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): ChatId = ChatId(Uuid.parse(decoder.decodeString()))
}
