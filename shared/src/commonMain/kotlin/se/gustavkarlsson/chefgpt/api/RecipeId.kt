package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.UuidValueSerializer
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable(with = RecipeIdSerializer::class)
@JvmInline
value class RecipeId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): RecipeId = RecipeId(Uuid.random())

        fun parse(uuidString: String): RecipeId = RecipeId(Uuid.parse(uuidString))

        fun parseOrNull(uuidString: String): RecipeId? = Uuid.parseOrNull(uuidString)?.let(::RecipeId)
    }
}

object RecipeIdSerializer : UuidValueSerializer<RecipeId>("recipe-id", ::RecipeId, RecipeId::value)
