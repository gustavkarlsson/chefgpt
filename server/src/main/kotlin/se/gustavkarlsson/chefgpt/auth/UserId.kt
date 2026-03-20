package se.gustavkarlsson.chefgpt.auth

import se.gustavkarlsson.chefgpt.chats.ChatId
import kotlin.uuid.Uuid

@JvmInline
value class UserId(
    val value: Uuid,
) {
    companion object {
        fun random(): UserId = UserId(Uuid.random())
    }
}
