package se.gustavkarlsson.chefgpt.facts

import se.gustavkarlsson.chefgpt.api.facts.v1.ApiTemperatureUnit
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiUserFacts
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiVolumeUnits
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiWeightUnits

/**
 * The unit the user measures temperature in.
 */
enum class TemperatureUnit {
    Celsius,
    Fahrenheit,
}

/**
 * The unit system the user measures weight in: metric (grams, kilograms) or US imperial
 * (ounces, pounds).
 */
enum class WeightUnits {
    Metric,
    UsImperial,
}

/**
 * The unit system the user measures volume in: metric (milliliters, liters) or US customary
 * (cups, tablespoons, fluid ounces).
 */
enum class VolumeUnits {
    Metric,
    UsCustomary,
}

/**
 * The facts the backend agents know about a user, as the app models them.
 *
 * A null field means the fact is unknown. [dietary] is null when unknown, empty when the user has
 * no restrictions (none), and non-empty otherwise.
 */
data class UserFacts(
    val preferredName: String?,
    val temperatureUnit: TemperatureUnit?,
    val weightUnits: WeightUnits?,
    val volumeUnits: VolumeUnits?,
    val dietary: Set<String>?,
) {
    val hasUnknown: Boolean
        get() = null in listOf(preferredName, temperatureUnit, weightUnits, volumeUnits, dietary)

    companion object {
        val Empty: UserFacts = UserFacts(null, null, null, null, null)
    }
}

fun ApiUserFacts.toDomainFacts(): UserFacts =
    UserFacts(
        preferredName = preferredName,
        temperatureUnit = temperatureUnit?.toDomainOrNull(),
        weightUnits = weightUnits?.toDomainOrNull(),
        volumeUnits = volumeUnits?.toDomainOrNull(),
        dietary = dietary,
    )

private fun ApiTemperatureUnit.toDomainOrNull(): TemperatureUnit =
    when (this) {
        ApiTemperatureUnit.Celsius -> TemperatureUnit.Celsius
        ApiTemperatureUnit.Fahrenheit -> TemperatureUnit.Fahrenheit
    }

private fun ApiWeightUnits.toDomainOrNull(): WeightUnits =
    when (this) {
        ApiWeightUnits.Metric -> WeightUnits.Metric
        ApiWeightUnits.UsImperial -> WeightUnits.UsImperial
    }

private fun ApiVolumeUnits.toDomainOrNull(): VolumeUnits =
    when (this) {
        ApiVolumeUnits.Metric -> VolumeUnits.Metric
        ApiVolumeUnits.UsCustomary -> VolumeUnits.UsCustomary
    }

fun UserFacts.toApi(): ApiUserFacts =
    ApiUserFacts(
        preferredName = preferredName,
        temperatureUnit = temperatureUnit?.toApi(),
        weightUnits = weightUnits?.toApi(),
        volumeUnits = volumeUnits?.toApi(),
        dietary = dietary,
    )

private fun TemperatureUnit.toApi(): ApiTemperatureUnit =
    when (this) {
        TemperatureUnit.Celsius -> ApiTemperatureUnit.Celsius
        TemperatureUnit.Fahrenheit -> ApiTemperatureUnit.Fahrenheit
    }

private fun WeightUnits.toApi(): ApiWeightUnits =
    when (this) {
        WeightUnits.Metric -> ApiWeightUnits.Metric
        WeightUnits.UsImperial -> ApiWeightUnits.UsImperial
    }

private fun VolumeUnits.toApi(): ApiVolumeUnits =
    when (this) {
        VolumeUnits.Metric -> ApiVolumeUnits.Metric
        VolumeUnits.UsCustomary -> ApiVolumeUnits.UsCustomary
    }
