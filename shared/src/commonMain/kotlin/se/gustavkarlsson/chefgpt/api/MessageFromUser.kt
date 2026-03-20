package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable
sealed interface MessageFromUser {
    @Serializable
    data object Waiting : MessageFromUser

    @Serializable
    data class Regular(
        val text: String?,
        val imageFileId: FileId? = null,
    ) : MessageFromUser
}

@Serializable
@JvmInline
value class FileId(
    val value: Uuid,
) {
    companion object {
        fun random(): FileId = FileId(Uuid.random())
    }
}
