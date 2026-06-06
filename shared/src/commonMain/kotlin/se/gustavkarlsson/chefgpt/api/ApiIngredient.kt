package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@SerialName("api-ingredient")
data class ApiIngredient(
    val name: String,
    val lastModified: Instant,
    val inInventory: Boolean,
)
