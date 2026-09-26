package se.gustavkarlsson.chefgpt.facts

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryFactRepositoryTest {
    private val userId = UserId.random()
    private val otherUserId = UserId.random()
    private val repository = InMemoryFactRepository()

    @Test
    fun `getFacts returns empty for a new user`() =
        runTest {
            val facts = repository.getFacts(userId)

            assertEquals(UserFacts.Empty, facts)
        }

    @Test
    fun `updateFacts returns the updated facts`() =
        runTest {
            val updated = repository.updateFacts(userId, UserFactsUpdate(weightUnits = WeightUnits.Metric))

            assertEquals(WeightUnits.Metric, updated.weightUnits)
        }

    @Test
    fun `updateFacts persists facts`() =
        runTest {
            repository.updateFacts(userId, UserFactsUpdate(weightUnits = WeightUnits.Metric))

            assertEquals(WeightUnits.Metric, repository.getFacts(userId).weightUnits)
        }

    @Test
    fun `updateFacts accumulates dietary restrictions across calls`() =
        runTest {
            repository.updateFacts(userId, UserFactsUpdate(addDietary = setOf("vegetarian")))
            repository.updateFacts(userId, UserFactsUpdate(addDietary = setOf("gluten-free")))

            assertEquals(setOf("vegetarian", "gluten-free"), repository.getFacts(userId).dietary)
        }

    @Test
    fun `facts are independent per user`() =
        runTest {
            repository.updateFacts(userId, UserFactsUpdate(weightUnits = WeightUnits.Metric))

            assertEquals(UserFacts.Empty, repository.getFacts(otherUserId))
        }

    @Test
    fun `replaceFacts overwrites all facts`() =
        runTest {
            repository.updateFacts(
                userId,
                UserFactsUpdate(weightUnits = WeightUnits.Metric, addDietary = setOf("vegetarian")),
            )

            val replacement =
                UserFacts(
                    "Gustav",
                    TemperatureUnit.Fahrenheit,
                    WeightUnits.UsImperial,
                    VolumeUnits.UsCustomary,
                    setOf("vegan"),
                )
            repository.replaceFacts(userId, replacement)

            assertEquals(replacement, repository.getFacts(userId))
        }

    @Test
    fun `replaceFacts sets dietary to none`() =
        runTest {
            val replacement =
                UserFacts(null, TemperatureUnit.Celsius, WeightUnits.Metric, VolumeUnits.Metric, emptySet())
            repository.replaceFacts(userId, replacement)

            assertEquals(emptySet(), repository.getFacts(userId).dietary)
        }
}
