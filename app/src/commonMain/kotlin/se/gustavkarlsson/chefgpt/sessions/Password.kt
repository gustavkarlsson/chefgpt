package se.gustavkarlsson.chefgpt.sessions

import kotlin.jvm.JvmInline

@JvmInline
value class Password(
    val value: String,
) {
    override fun toString(): String = value
}
