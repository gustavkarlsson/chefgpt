package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.UuidValueSerializer
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable(with = JobIdSerializer::class)
@JvmInline
value class JobId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): JobId = JobId(Uuid.random())

        fun parse(uuidString: String): JobId = JobId(Uuid.parse(uuidString))

        fun parseOrNull(uuidString: String): JobId? = Uuid.parseOrNull(uuidString)?.let(::JobId)
    }
}

object JobIdSerializer : UuidValueSerializer<JobId>("job-id", ::JobId, JobId::value)
