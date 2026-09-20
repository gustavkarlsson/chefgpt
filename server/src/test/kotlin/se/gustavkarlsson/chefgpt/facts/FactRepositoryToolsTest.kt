package se.gustavkarlsson.chefgpt.facts

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class FactRepositoryToolsTest {
    private val userId = UserId.random()
    private val repository = InMemoryFactRepository()
    private val tools = FactRepositoryTools(repository, userId)

    @Test
    fun `setPreferredName remembers the name`() =
        runTest {
            tools.setPreferredName("Gustav")

            assertEquals("Gustav", repository.getFacts(userId).preferredName)
        }

    @Test
    fun `setUnitSystem remembers the unit system`() =
        runTest {
            tools.setUnitSystem(UnitSystem.Imperial)

            assertEquals(UnitSystem.Imperial, repository.getFacts(userId).unitSystem)
        }

    @Test
    fun `setMeasurement remembers the measurement`() =
        runTest {
            tools.setMeasurement(Measurement.Weight)

            assertEquals(Measurement.Weight, repository.getFacts(userId).measurement)
        }

    @Test
    fun `setTemperature remembers the temperature unit`() =
        runTest {
            tools.setTemperature(TemperatureUnit.Fahrenheit)

            assertEquals(TemperatureUnit.Fahrenheit, repository.getFacts(userId).temperature)
        }

    @Test
    fun `addDietaryRestrictions adds without removing others`() =
        runTest {
            tools.addDietaryRestrictions(listOf("vegetarian"))
            tools.addDietaryRestrictions(listOf("gluten-free"))

            assertEquals(setOf("vegetarian", "gluten-free"), repository.getFacts(userId).dietary)
        }

    @Test
    fun `removeDietaryRestrictions removes a restriction`() =
        runTest {
            tools.addDietaryRestrictions(listOf("vegetarian"))
            tools.removeDietaryRestrictions(listOf("vegetarian"))

            assertEquals(emptySet(), repository.getFacts(userId).dietary)
        }
}
