package se.gustavkarlsson.chefgpt.facts

import kotlinx.serialization.Serializable

@Serializable
enum class UnitSystem {
    Metric,
    Imperial,
}

@Serializable
enum class Measurement {
    Weight,
    Volume,
}

@Serializable
enum class TemperatureUnit {
    Celsius,
    Fahrenheit,
}

/**
 * Everything the backend agents know about a user.
 *
 * A null scalar means the fact has not been established (unknown). [dietary] is null when unknown,
 * empty when the user has said they have no restrictions (none), and non-empty when restrictions
 * are known.
 */
@Serializable
data class UserFacts(
    val preferredName: String?,
    val unitSystem: UnitSystem?,
    val measurement: Measurement?,
    val temperature: TemperatureUnit?,
    val dietary: Set<String>?,
) {
    companion object {
        val UNKNOWN =
            UserFacts(
                preferredName = null,
                unitSystem = null,
                measurement = null,
                temperature = null,
                dietary = null,
            )
    }
}

/**
 * A patch to [UserFacts]. Absence means "leave unchanged": null for the scalar facts, and empty
 * sets for the dietary add/remove sets.
 */
data class UserFactsUpdate(
    val preferredName: String? = null,
    val unitSystem: UnitSystem? = null,
    val measurement: Measurement? = null,
    val temperature: TemperatureUnit? = null,
    val addDietary: Set<String> = emptySet(),
    val removeDietary: Set<String> = emptySet(),
)

fun UserFacts.applyUpdate(update: UserFactsUpdate): UserFacts =
    UserFacts(
        preferredName = update.preferredName ?: preferredName,
        unitSystem = update.unitSystem ?: unitSystem,
        measurement = update.measurement ?: measurement,
        temperature = update.temperature ?: temperature,
        dietary = applyDietary(update),
    )

private fun UserFacts.applyDietary(update: UserFactsUpdate): Set<String>? {
    val current = dietary
    if (current == null && update.addDietary.isEmpty()) {
        return null
    }
    return (current.orEmpty() - update.removeDietary) + update.addDietary
}

fun UserFacts.toPromptText(): String =
    buildString {
        appendLine("Facts about the user ('unknown' means you should ask before relying on it):")
        appendLine("- Preferred name: ${preferredName ?: "unknown"}")
        appendLine("- Unit system: ${unitSystem?.name?.lowercase() ?: "unknown"}")
        appendLine("- Measurement: ${measurement?.name?.lowercase() ?: "unknown"}")
        appendLine("- Temperature: ${temperature?.name?.lowercase() ?: "unknown"}")
        appendLine("- Dietary restrictions: ${dietaryText()}")
    }

fun UserFacts.toMeasurementPromptText(): String =
    buildString {
        appendLine(
            "The user's measurement preferences. Apply these when writing amounts; where a value is unknown, keep the recipe's original measurement:",
        )
        appendLine("- Unit system: ${unitSystem?.name?.lowercase() ?: "unknown"}")
        appendLine("- Measurement: ${measurement?.name?.lowercase() ?: "unknown"}")
        appendLine("- Temperature: ${temperature?.name?.lowercase() ?: "unknown"}")
    }

private fun UserFacts.dietaryText(): String =
    when (val restrictions = dietary) {
        null -> "unknown"
        else -> restrictions.sorted().joinToString(", ").ifEmpty { "none" }
    }
