package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The user's temperature unit, as the client sees it.
 */
@Serializable
enum class ApiTemperatureUnit {
    @SerialName("celsius")
    Celsius,

    @SerialName("fahrenheit")
    Fahrenheit,
}
