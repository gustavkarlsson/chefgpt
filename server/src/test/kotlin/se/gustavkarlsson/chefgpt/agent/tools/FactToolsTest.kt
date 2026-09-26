package se.gustavkarlsson.chefgpt.agent.tools

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.facts.InMemoryFactRepository
import se.gustavkarlsson.chefgpt.facts.TemperatureUnit
import se.gustavkarlsson.chefgpt.facts.VolumeUnits
import se.gustavkarlsson.chefgpt.facts.WeightUnits
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
    fun `setWeightUnits remembers the weight units`() =
        runTest {
            SetWeightUnitsTool(repository, userId).setWeightUnits(WeightUnits.UsImperial)

            assertEquals(WeightUnits.UsImperial, repository.getFacts(userId).weightUnits)
        }

    @Test
    fun `setVolumeUnits remembers the volume units`() =
        runTest {
            SetVolumeUnitsTool(repository, userId).setVolumeUnits(VolumeUnits.UsCustomary)

            assertEquals(VolumeUnits.UsCustomary, repository.getFacts(userId).volumeUnits)
        }

    @Test
    fun `setTemperatureUnit remembers the temperature unit`() =
        runTest {
            SetTemperatureUnitTool(repository, userId).setTemperatureUnit(TemperatureUnit.Fahrenheit)

            assertEquals(TemperatureUnit.Fahrenheit, repository.getFacts(userId).temperatureUnit)
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
            assertNull(GetWeightUnitsTool(repository, userId).getWeightUnits())
            assertNull(GetVolumeUnitsTool(repository, userId).getVolumeUnits())
            assertNull(GetTemperatureUnitTool(repository, userId).getTemperatureUnit())
            assertNull(GetDietaryRestrictionsTool(repository, userId).getDietaryRestrictions())
        }

    @Test
    fun `getPreferredName returns the name once set`() =
        runTest {
            SetPreferredNameTool(repository, userId).setPreferredName("Gustav")

            assertEquals("Gustav", GetPreferredNameTool(repository, userId).getPreferredName())
        }

    @Test
    fun `getWeightUnits returns the weight units once set`() =
        runTest {
            SetWeightUnitsTool(repository, userId).setWeightUnits(WeightUnits.UsImperial)

            assertEquals(WeightUnits.UsImperial, GetWeightUnitsTool(repository, userId).getWeightUnits())
        }

    @Test
    fun `getVolumeUnits returns the volume units once set`() =
        runTest {
            SetVolumeUnitsTool(repository, userId).setVolumeUnits(VolumeUnits.UsCustomary)

            assertEquals(VolumeUnits.UsCustomary, GetVolumeUnitsTool(repository, userId).getVolumeUnits())
        }

    @Test
    fun `getTemperatureUnit returns the temperature unit once set`() =
        runTest {
            SetTemperatureUnitTool(repository, userId).setTemperatureUnit(TemperatureUnit.Fahrenheit)

            assertEquals(TemperatureUnit.Fahrenheit, GetTemperatureUnitTool(repository, userId).getTemperatureUnit())
        }

    @Test
    fun `getDietaryRestrictions returns the restrictions once added`() =
        runTest {
            AddDietaryRestrictionsTool(repository, userId).addDietaryRestrictions(listOf("vegetarian"))

            assertEquals(setOf("vegetarian"), GetDietaryRestrictionsTool(repository, userId).getDietaryRestrictions())
        }
}
