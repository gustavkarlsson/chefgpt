package se.gustavkarlsson.chefgpt.facts

import se.gustavkarlsson.chefgpt.api.facts.v1.ApiTemperatureUnit
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiUserFacts
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiVolumeUnits
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiWeightUnits

fun UserFacts.toApi(): ApiUserFacts =
    ApiUserFacts(
        preferredName = preferredName,
        temperatureUnit = temperatureUnit?.toApi(),
        weightUnits = weightUnits?.toApi(),
        volumeUnits = volumeUnits?.toApi(),
        dietary = dietary,
    )

fun ApiUserFacts.toDomain(): UserFacts =
    UserFacts(
        preferredName = preferredName,
        temperatureUnit = temperatureUnit?.toDomain(),
        weightUnits = weightUnits?.toDomain(),
        volumeUnits = volumeUnits?.toDomain(),
        dietary = dietary,
    )

fun TemperatureUnit.toApi(): ApiTemperatureUnit =
    when (this) {
        TemperatureUnit.Celsius -> ApiTemperatureUnit.Celsius
        TemperatureUnit.Fahrenheit -> ApiTemperatureUnit.Fahrenheit
    }

fun WeightUnits.toApi(): ApiWeightUnits =
    when (this) {
        WeightUnits.Metric -> ApiWeightUnits.Metric
        WeightUnits.UsImperial -> ApiWeightUnits.UsImperial
    }

fun VolumeUnits.toApi(): ApiVolumeUnits =
    when (this) {
        VolumeUnits.Metric -> ApiVolumeUnits.Metric
        VolumeUnits.UsCustomary -> ApiVolumeUnits.UsCustomary
    }

fun ApiTemperatureUnit.toDomain(): TemperatureUnit =
    when (this) {
        ApiTemperatureUnit.Celsius -> TemperatureUnit.Celsius
        ApiTemperatureUnit.Fahrenheit -> TemperatureUnit.Fahrenheit
    }

fun ApiWeightUnits.toDomain(): WeightUnits =
    when (this) {
        ApiWeightUnits.Metric -> WeightUnits.Metric
        ApiWeightUnits.UsImperial -> WeightUnits.UsImperial
    }

fun ApiVolumeUnits.toDomain(): VolumeUnits =
    when (this) {
        ApiVolumeUnits.Metric -> VolumeUnits.Metric
        ApiVolumeUnits.UsCustomary -> VolumeUnits.UsCustomary
    }
