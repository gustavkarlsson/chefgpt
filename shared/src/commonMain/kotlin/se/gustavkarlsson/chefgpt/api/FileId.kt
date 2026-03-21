package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

// FIXME serialize as string
@Serializable
@JvmInline
value class FileId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun parseOrNull(value: String): FileId? = Uuid.parseOrNull(value)?.let(::FileId)

        fun random(): FileId = FileId(Uuid.random())
    }
}
