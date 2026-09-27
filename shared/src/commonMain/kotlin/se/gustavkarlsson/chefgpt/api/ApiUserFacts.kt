package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable

// TODO Are these enums safe?

/**
 * The facts the backend agents remember about a user, as the client sees them.
 */
@Serializable
data class ApiUserFacts(
    val preferredName: String?,
    val temperatureUnit: ApiTemperatureUnit?,
    val weightUnits: ApiWeightUnits?,
    val volumeUnits: ApiVolumeUnits?,
    val dietary: Set<String>?,
)
