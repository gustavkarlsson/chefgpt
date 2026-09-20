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
    fun `getFacts returns unknown for a new user`() =
        runTest {
            val facts = repository.getFacts(userId)

            assertEquals(UserFacts.UNKNOWN, facts)
        }

    @Test
    fun `updateFacts returns the updated facts`() =
        runTest {
            val updated = repository.updateFacts(userId, UserFactsUpdate(unitSystem = UnitSystem.Metric))

            assertEquals(UnitSystem.Metric, updated.unitSystem)
        }

    @Test
    fun `updateFacts persists facts`() =
        runTest {
            repository.updateFacts(userId, UserFactsUpdate(unitSystem = UnitSystem.Metric))

            assertEquals(UnitSystem.Metric, repository.getFacts(userId).unitSystem)
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
            repository.updateFacts(userId, UserFactsUpdate(unitSystem = UnitSystem.Metric))

            assertEquals(UserFacts.UNKNOWN, repository.getFacts(otherUserId))
        }
}
