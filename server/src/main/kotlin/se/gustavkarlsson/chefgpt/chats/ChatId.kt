package se.gustavkarlsson.chefgpt.chats

import kotlin.uuid.Uuid

// TODO Make serializable (as primitive string) and add to shared API models. Use directly in server and client instead of unwrapping/wrapping the value
@JvmInline
value class ChatId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): ChatId = ChatId(Uuid.random())
    }
}
