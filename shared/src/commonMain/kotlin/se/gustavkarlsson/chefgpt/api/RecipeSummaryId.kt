package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.UuidValueSerializer
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable(with = RecipeSummaryIdSerializer::class)
@JvmInline
value class RecipeSummaryId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): RecipeSummaryId = RecipeSummaryId(Uuid.random())

        fun parse(uuidString: String): RecipeSummaryId = RecipeSummaryId(Uuid.parse(uuidString))

        fun parseOrNull(uuidString: String): RecipeSummaryId? = Uuid.parseOrNull(uuidString)?.let(::RecipeSummaryId)
    }
}

object RecipeSummaryIdSerializer : UuidValueSerializer<RecipeSummaryId>(
    "recipe-summary-id",
    ::RecipeSummaryId,
    RecipeSummaryId::value,
)
