package se.gustavkarlsson.chefgpt.facts

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
 * The facts the backend agents know about a user.
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
    companion object {
        val Empty: UserFacts = UserFacts(null, null, null, null, null)
    }
}

/**
 * A patch to [UserFacts]. Absence means "leave unchanged": null for the scalar facts, and empty
 * sets for the dietary add/remove sets.
 */
data class UserFactsUpdate(
    val preferredName: String? = null,
    val temperatureUnit: TemperatureUnit? = null,
    val weightUnits: WeightUnits? = null,
    val volumeUnits: VolumeUnits? = null,
    val addDietary: Set<String> = emptySet(),
    val removeDietary: Set<String> = emptySet(),
)

fun UserFacts.applyUpdate(update: UserFactsUpdate): UserFacts =
    UserFacts(
        preferredName = update.preferredName ?: preferredName,
        temperatureUnit = update.temperatureUnit ?: temperatureUnit,
        weightUnits = update.weightUnits ?: weightUnits,
        volumeUnits = update.volumeUnits ?: volumeUnits,
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
        appendLine(toUnitText())
        appendLine("- Dietary restrictions: ${dietaryText()}")
    }

fun UserFacts.toMeasurementPromptText(): String =
    buildString {
        appendLine(
            "The user's measurement preferences. Apply these when writing amounts; " +
                "where a value is unknown, keep the recipe's original measurement:",
        )
        appendLine(toUnitText())
    }

private fun UserFacts.toUnitText(): String =
    buildString {
        appendLine("- Preferred name: ${preferredName ?: "unknown"}")
        val temperatureUnitDescription =
            when (temperatureUnit) {
                TemperatureUnit.Celsius -> "Celsius (C)"
                TemperatureUnit.Fahrenheit -> "Fahrenheit (F)"
                null -> "unknown"
            }

        appendLine("- Temperature unit: $temperatureUnitDescription")
        val weightUnitsDescription =
            when (weightUnits) {
                WeightUnits.Metric -> "Metric: grams (g) and kilograms (kg)"
                WeightUnits.UsImperial -> "US / Imperial: ounces (oz) and pounds (lb)"
                null -> "unknown"
            }
        appendLine("- Weight units: $weightUnitsDescription")
        val volumeUnitsDescription =
            when (volumeUnits) {
                VolumeUnits.Metric -> {
                    "Metric: teaspoon (tsp), tablespoon (tbsp), milliliters (ml), liters (l)"
                }

                VolumeUnits.UsCustomary -> {
                    "US Customary: teaspoon (tsp), tablespoon (tbsp), " +
                        "fluid ounce (fl oz), cup (C)"
                }

                null -> {
                    "unknown"
                }
            }
        append("- Volume Units: $volumeUnitsDescription")
    }

private fun UserFacts.dietaryText(): String =
    when (val restrictions = dietary) {
        null -> "unknown"
        else -> restrictions.sorted().joinToString(", ").ifEmpty { "none" }
    }
