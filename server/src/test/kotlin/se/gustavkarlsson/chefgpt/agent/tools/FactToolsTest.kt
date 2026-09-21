package se.gustavkarlsson.chefgpt.agent.tools

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.facts.InMemoryFactRepository
import se.gustavkarlsson.chefgpt.facts.Measurement
import se.gustavkarlsson.chefgpt.facts.TemperatureUnit
import se.gustavkarlsson.chefgpt.facts.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FactToolsTest {
    private val userId = UserId.random()
    private val repository = InMemoryFactRepository()

    @Test
    fun `setPreferredName remembers the name`() =
        runTest {
            SetPreferredNameTool(repository, userId).setPreferredName("Gustav")

            assertEquals("Gustav", repository.getFacts(userId).preferredName)
        }

    @Test
    fun `setUnitSystem remembers the unit system`() =
        runTest {
            SetUnitSystemTool(repository, userId).setUnitSystem(UnitSystem.Imperial)

            assertEquals(UnitSystem.Imperial, repository.getFacts(userId).unitSystem)
        }

    @Test
    fun `setMeasurement remembers the measurement`() =
        runTest {
            SetMeasurementTool(repository, userId).setMeasurement(Measurement.Weight)

            assertEquals(Measurement.Weight, repository.getFacts(userId).measurement)
        }

    @Test
    fun `setTemperature remembers the temperature unit`() =
        runTest {
            SetTemperatureTool(repository, userId).setTemperature(TemperatureUnit.Fahrenheit)

            assertEquals(TemperatureUnit.Fahrenheit, repository.getFacts(userId).temperature)
        }

    @Test
    fun `addDietaryRestrictions adds without removing others`() =
        runTest {
            val tool = AddDietaryRestrictionsTool(repository, userId)

            tool.addDietaryRestrictions(listOf("vegetarian"))
            tool.addDietaryRestrictions(listOf("gluten-free"))

            assertEquals(setOf("vegetarian", "gluten-free"), repository.getFacts(userId).dietary)
        }

    @Test
    fun `removeDietaryRestrictions removes a restriction`() =
        runTest {
            AddDietaryRestrictionsTool(repository, userId).addDietaryRestrictions(listOf("vegetarian"))
            RemoveDietaryRestrictionsTool(repository, userId).removeDietaryRestrictions(listOf("vegetarian"))

            assertEquals(emptySet(), repository.getFacts(userId).dietary)
        }

    @Test
    fun `getters return null before any fact is set`() =
        runTest {
            assertNull(GetPreferredNameTool(repository, userId).getPreferredName())
            assertNull(GetUnitSystemTool(repository, userId).getUnitSystem())
            assertNull(GetMeasurementTool(repository, userId).getMeasurement())
            assertNull(GetTemperatureTool(repository, userId).getTemperature())
            assertNull(GetDietaryRestrictionsTool(repository, userId).getDietaryRestrictions())
        }

    @Test
    fun `getPreferredName returns the name once set`() =
        runTest {
            SetPreferredNameTool(repository, userId).setPreferredName("Gustav")

            assertEquals("Gustav", GetPreferredNameTool(repository, userId).getPreferredName())
        }

    @Test
    fun `getUnitSystem returns the unit system once set`() =
        runTest {
            SetUnitSystemTool(repository, userId).setUnitSystem(UnitSystem.Imperial)

            assertEquals(UnitSystem.Imperial, GetUnitSystemTool(repository, userId).getUnitSystem())
        }

    @Test
    fun `getMeasurement returns the measurement once set`() =
        runTest {
            SetMeasurementTool(repository, userId).setMeasurement(Measurement.Weight)

            assertEquals(Measurement.Weight, GetMeasurementTool(repository, userId).getMeasurement())
        }

    @Test
    fun `getTemperature returns the temperature unit once set`() =
        runTest {
            SetTemperatureTool(repository, userId).setTemperature(TemperatureUnit.Fahrenheit)

            assertEquals(TemperatureUnit.Fahrenheit, GetTemperatureTool(repository, userId).getTemperature())
        }

    @Test
    fun `getDietaryRestrictions returns the restrictions once added`() =
        runTest {
            AddDietaryRestrictionsTool(repository, userId).addDietaryRestrictions(listOf("vegetarian"))

            assertEquals(setOf("vegetarian"), GetDietaryRestrictionsTool(repository, userId).getDietaryRestrictions())
        }
}
