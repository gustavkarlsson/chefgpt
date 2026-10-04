package se.gustavkarlsson.chefgpt.agent.tools.models

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.facts.TemperatureUnit
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.facts.VolumeUnits
import se.gustavkarlsson.chefgpt.facts.WeightUnits

@Serializable
@LLMDescription("The unit the user measures temperature in: Celsius or Fahrenheit.")
enum class ToolTemperatureUnit {
    Celsius,
    Fahrenheit,
}

@Serializable
@LLMDescription(
    "The unit system the user measures weight in: Metric (grams, kilograms) or " +
        "UsImperial (ounces, pounds).",
)
enum class ToolWeightUnits {
    Metric,
    UsImperial,
}

@Serializable
@LLMDescription(
    "The unit system the user measures volume in: Metric (milliliters, liters) or " +
        "UsCustomary (cups, tablespoons, fluid ounces).",
)
enum class ToolVolumeUnits {
    Metric,
    UsCustomary,
}

@Serializable
@LLMDescription("The facts the agent knows about the user. null means the fact is unknown.")
data class ToolUserFacts(
    @property:LLMDescription("What the user wants to be called, or null if unknown.")
    val preferredName: String?,
    @property:LLMDescription("The unit the user measures temperature in, or null if unknown.")
    val temperatureUnit: ToolTemperatureUnit?,
    @property:LLMDescription("The unit system the user measures weight in, or null if unknown.")
    val weightUnits: ToolWeightUnits?,
    @property:LLMDescription("The unit system the user measures volume in, or null if unknown.")
    val volumeUnits: ToolVolumeUnits?,
    @property:LLMDescription(
        "The user's dietary restrictions, e.g. ['vegetarian', 'gluten-free']. " +
            "null means unknown, and empty means they have said they have none.",
    )
    val dietary: Set<String>?,
)

fun UserFacts.toTool(): ToolUserFacts =
    ToolUserFacts(
        preferredName = preferredName,
        temperatureUnit = temperatureUnit?.toTool(),
        weightUnits = weightUnits?.toTool(),
        volumeUnits = volumeUnits?.toTool(),
        dietary = dietary,
    )

fun TemperatureUnit.toTool(): ToolTemperatureUnit =
    when (this) {
        TemperatureUnit.Celsius -> ToolTemperatureUnit.Celsius
        TemperatureUnit.Fahrenheit -> ToolTemperatureUnit.Fahrenheit
    }

fun WeightUnits.toTool(): ToolWeightUnits =
    when (this) {
        WeightUnits.Metric -> ToolWeightUnits.Metric
        WeightUnits.UsImperial -> ToolWeightUnits.UsImperial
    }

fun VolumeUnits.toTool(): ToolVolumeUnits =
    when (this) {
        VolumeUnits.Metric -> ToolVolumeUnits.Metric
        VolumeUnits.UsCustomary -> ToolVolumeUnits.UsCustomary
    }

fun ToolTemperatureUnit.toDomain(): TemperatureUnit =
    when (this) {
        ToolTemperatureUnit.Celsius -> TemperatureUnit.Celsius
        ToolTemperatureUnit.Fahrenheit -> TemperatureUnit.Fahrenheit
    }

fun ToolWeightUnits.toDomain(): WeightUnits =
    when (this) {
        ToolWeightUnits.Metric -> WeightUnits.Metric
        ToolWeightUnits.UsImperial -> WeightUnits.UsImperial
    }

fun ToolVolumeUnits.toDomain(): VolumeUnits =
    when (this) {
        ToolVolumeUnits.Metric -> VolumeUnits.Metric
        ToolVolumeUnits.UsCustomary -> VolumeUnits.UsCustomary
    }
