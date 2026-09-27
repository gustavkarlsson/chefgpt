package se.gustavkarlsson.chefgpt.facts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserFactsTest {
    private val empty = UserFacts.Empty

    @Test
    fun `applyUpdate sets a scalar fact`() {
        val updated = empty.applyUpdate(UserFactsUpdate(weightUnits = WeightUnits.Metric))

        assertEquals(WeightUnits.Metric, updated.weightUnits)
    }

    @Test
    fun `applyUpdate leaves unspecified facts unchanged`() {
        val current = empty.applyUpdate(UserFactsUpdate(weightUnits = WeightUnits.Metric))

        val updated = current.applyUpdate(UserFactsUpdate(volumeUnits = VolumeUnits.UsCustomary))

        assertEquals(WeightUnits.Metric, updated.weightUnits)
        assertEquals(VolumeUnits.UsCustomary, updated.volumeUnits)
    }

    @Test
    fun `applyUpdate adds a dietary restriction to an unknown user`() {
        val updated = empty.applyUpdate(UserFactsUpdate(addDietary = setOf("vegetarian")))

        assertEquals(setOf("vegetarian"), updated.dietary)
    }

    @Test
    fun `applyUpdate keeps unrelated restrictions when adding one`() {
        val current = empty.applyUpdate(UserFactsUpdate(addDietary = setOf("gluten-free")))

        val updated = current.applyUpdate(UserFactsUpdate(addDietary = setOf("vegan")))

        assertEquals(setOf("gluten-free", "vegan"), updated.dietary)
    }

    @Test
    fun `applyUpdate removes a dietary restriction`() {
        val current = empty.applyUpdate(UserFactsUpdate(addDietary = setOf("vegetarian")))

        val updated = current.applyUpdate(UserFactsUpdate(removeDietary = setOf("vegetarian")))

        assertEquals(emptySet(), updated.dietary)
    }

    @Test
    fun `applyUpdate leaves dietary unknown when only removing from an unknown user`() {
        val updated = empty.applyUpdate(UserFactsUpdate(removeDietary = setOf("vegetarian")))

        assertNull(updated.dietary)
    }

    @Test
    fun `prompt text shows unknown for every fact of a new user`() {
        val text = empty.toPromptText()

        assertEquals(
            """
            Facts about the user ('unknown' means you should ask before relying on it):
            - Preferred name: unknown
            - Temperature unit: unknown
            - Weight units: unknown
            - Volume Units: unknown
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
                temperatureUnit = TemperatureUnit.Celsius,
                weightUnits = WeightUnits.Metric,
                volumeUnits = VolumeUnits.UsCustomary,
                dietary = setOf("vegan", "gluten-free"),
            )

        val text = facts.toPromptText()

        assertEquals(
            """
            Facts about the user ('unknown' means you should ask before relying on it):
            - Preferred name: Gustav
            - Temperature unit: Celsius (C)
            - Weight units: Metric: grams (g) and kilograms (kg)
            - Volume Units: US Customary: teaspoon (tsp), tablespoon (tbsp), fluid ounce (fl oz), cup (C)
            - Dietary restrictions: gluten-free, vegan
            """.trimIndent(),
            text.trim(),
        )
    }
}
