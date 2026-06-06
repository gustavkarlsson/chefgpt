package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.UuidValueSerializer
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable(with = IngredientIdSerializer::class)
@JvmInline
value class IngredientId(
    val value: Uuid,
) {
    override fun toString(): String = value.toString()

    companion object {
        fun random(): IngredientId = IngredientId(Uuid.random())

        fun parse(uuidString: String): IngredientId = IngredientId(Uuid.parse(uuidString))

        fun parseOrNull(uuidString: String): IngredientId? = Uuid.parseOrNull(uuidString)?.let(::IngredientId)
    }
}

object IngredientIdSerializer : UuidValueSerializer<IngredientId>("ingredient-id", ::IngredientId, IngredientId::value)
