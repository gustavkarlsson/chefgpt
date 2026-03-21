package se.gustavkarlsson.chefgpt.chats

import kotlin.uuid.Uuid

@JvmInline
value class ChatId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): ChatId = ChatId(Uuid.random())
    }
}
