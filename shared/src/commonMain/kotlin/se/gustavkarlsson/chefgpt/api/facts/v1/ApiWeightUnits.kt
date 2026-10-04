package se.gustavkarlsson.chefgpt.api.facts.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The user's weight units, as the client sees them.
 */
@Serializable
enum class ApiWeightUnits {
    @SerialName("metric")
    Metric,

    @SerialName("us_imperial")
    UsImperial,
}
