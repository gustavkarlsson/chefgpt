package se.gustavkarlsson.chefgpt.facts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserFactsTest {
    private val unknown = UserFacts.UNKNOWN

    @Test
    fun `applyUpdate sets a scalar fact`() {
        val updated = unknown.applyUpdate(UserFactsUpdate(unitSystem = UnitSystem.Metric))

        assertEquals(UnitSystem.Metric, updated.unitSystem)
    }

    @Test
    fun `applyUpdate leaves unspecified facts unchanged`() {
        val current = unknown.applyUpdate(UserFactsUpdate(unitSystem = UnitSystem.Metric))

        val updated = current.applyUpdate(UserFactsUpdate(measurement = Measurement.Weight))

        assertEquals(UnitSystem.Metric, updated.unitSystem)
        assertEquals(Measurement.Weight, updated.measurement)
    }

    @Test
    fun `applyUpdate adds a dietary restriction to an unknown user`() {
        val updated = unknown.applyUpdate(UserFactsUpdate(addDietary = setOf("vegetarian")))

        assertEquals(setOf("vegetarian"), updated.dietary)
    }

    @Test
    fun `applyUpdate keeps unrelated restrictions when adding one`() {
        val current = unknown.applyUpdate(UserFactsUpdate(addDietary = setOf("gluten-free")))

        val updated = current.applyUpdate(UserFactsUpdate(addDietary = setOf("vegan")))

        assertEquals(setOf("gluten-free", "vegan"), updated.dietary)
    }

    @Test
    fun `applyUpdate removes a dietary restriction`() {
        val current = unknown.applyUpdate(UserFactsUpdate(addDietary = setOf("vegetarian")))

        val updated = current.applyUpdate(UserFactsUpdate(removeDietary = setOf("vegetarian")))

        assertEquals(emptySet(), updated.dietary)
    }

    @Test
    fun `applyUpdate leaves dietary unknown when only removing from an unknown user`() {
        val updated = unknown.applyUpdate(UserFactsUpdate(removeDietary = setOf("vegetarian")))

        assertNull(updated.dietary)
    }

    @Test
    fun `prompt text shows unknown for every fact of a new user`() {
        val text = unknown.toPromptText()

        assertEquals(
            """
            Facts about the user ('unknown' means you should ask before relying on it):
            - Preferred name: unknown
            - Unit system: unknown
            - Measurement: unknown
            - Temperature: unknown
            - Dietary restrictions: unknown
            """.trimIndent(),
            text.trim(),
        )
    }

    @Test
    fun `prompt text shows known facts, sorted dietary, and none for empty dietary`() {
        val facts =
            UserFacts(
                preferredName = "Gustav",
                unitSystem = UnitSystem.Metric,
                measurement = Measurement.Weight,
                temperature = TemperatureUnit.Celsius,
                dietary = setOf("vegan", "gluten-free"),
            )

        val text = facts.toPromptText()

        assertEquals(
            """
            Facts about the user ('unknown' means you should ask before relying on it):
            - Preferred name: Gustav
            - Unit system: metric
            - Measurement: weight
            - Temperature: celsius
            - Dietary restrictions: gluten-free, vegan
            """.trimIndent(),
            text.trim(),
        )
    }
}
