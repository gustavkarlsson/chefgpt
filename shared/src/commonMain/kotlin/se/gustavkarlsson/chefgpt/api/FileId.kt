package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class FileId(
    val value: Uuid,
) {
    companion object {
        fun random(): FileId = FileId(Uuid.random())
    }
}
