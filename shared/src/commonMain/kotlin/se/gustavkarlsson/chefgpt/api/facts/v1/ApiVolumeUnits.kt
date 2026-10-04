package se.gustavkarlsson.chefgpt.api.facts.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The user's volume units, as the client sees them.
 */
@Serializable
enum class ApiVolumeUnits {
    @SerialName("metric")
    Metric,

    @SerialName("us_customary")
    UsCustomary,
}
