package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
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

        fun parseOrNull(uuidString: String): ChatId? = Uuid.parseOrNull(uuidString)?.let(::ChatId)
    }
}

object ChatIdSerializer : UuidValueSerializer<ChatId>("chat-id", ::ChatId, ChatId::value)
