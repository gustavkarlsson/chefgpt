package se.gustavkarlsson.chefgpt.sessions

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.StringValueSerializer
import kotlin.jvm.JvmInline

@Serializable(with = SessionIdSerializer::class)
@JvmInline
value class SessionId(
    val value: String,
) {
    override fun toString(): String = value
}

object SessionIdSerializer : StringValueSerializer<SessionId>("session-id", ::SessionId, SessionId::value)
