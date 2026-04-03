package se.gustavkarlsson.chefgpt.sessions

import kotlin.jvm.JvmInline

@JvmInline
value class UserName(
    val value: String,
) {
    override fun toString(): String = value
}
