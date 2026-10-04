package se.gustavkarlsson.chefgpt.api.ingredients.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.common.IngredientId
import kotlin.time.Instant

@Serializable
@SerialName("api-ingredient")
data class ApiIngredient(
    val id: IngredientId,
    val name: String,
    val lastModified: Instant,
    val inInventory: Boolean,
)
