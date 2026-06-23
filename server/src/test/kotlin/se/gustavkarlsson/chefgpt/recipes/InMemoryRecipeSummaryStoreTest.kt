package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.RecipeSummaryId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryRecipeSummaryStoreTest {
    private val userId = UserId.random()
    private val otherUserId = UserId.random()
    private val store = InMemoryRecipeSummaryStore()

    @Test
    fun `getRecipeSummaries returns empty list for new user`() =
        runTest {
            assertTrue(store.getRecipeSummaries(userId).isEmpty())
        }

    @Test
    fun `addRecipeSummary returns the stored recipe summary`() =
        runTest {
            val added = store.addRecipeSummary(userId, "Carbonara", SpoonacularId(1L), "https://img/c.jpg")

            assertEquals("Carbonara", added.title)
            assertEquals(SpoonacularId(1L), added.spoonacularId)
            assertEquals("https://img/c.jpg", added.imageUrl)
        }

    @Test
    fun `addRecipeSummary allows a null image`() =
        runTest {
            val added = store.addRecipeSummary(userId, "Carbonara", SpoonacularId(1L), null)

            assertNull(added.imageUrl)
        }

    @Test
    fun `addRecipeSummary persists the recipe summary`() =
        runTest {
            val added = store.addRecipeSummary(userId, "Carbonara", SpoonacularId(1L), null)

            assertEquals(listOf(added), store.getRecipeSummaries(userId))
        }

    @Test
    fun `addRecipeSummary assigns a unique id per recipe summary`() =
        runTest {
            val first = store.addRecipeSummary(userId, "Carbonara", SpoonacularId(1L), null)
            val second = store.addRecipeSummary(userId, "Carbonara", SpoonacularId(1L), null)

            assertTrue(first.id != second.id)
        }

    @Test
    fun `deleteRecipeSummary removes the recipe summary and returns true`() =
        runTest {
            val added = store.addRecipeSummary(userId, "Carbonara", SpoonacularId(1L), null)

            val deleted = store.deleteRecipeSummary(userId, added.id)

            assertTrue(deleted)
            assertTrue(store.getRecipeSummaries(userId).isEmpty())
        }

    @Test
    fun `deleteRecipeSummary returns false when the recipe summary does not exist`() =
        runTest {
            assertFalse(store.deleteRecipeSummary(userId, RecipeSummaryId.random()))
        }

    @Test
    fun `streamRecipeSummaries emits empty list for new user`() =
        runTest {
            assertTrue(store.streamRecipeSummaries(userId).first().isEmpty())
        }

    @Test
    fun `streamRecipeSummaries emits current recipe summaries`() =
        runTest {
            val added = store.addRecipeSummary(userId, "Carbonara", SpoonacularId(1L), null)

            assertEquals(listOf(added), store.streamRecipeSummaries(userId).first())
        }

    @Test
    fun `recipe summary store is independent per user`() =
        runTest {
            store.addRecipeSummary(userId, "Carbonara", SpoonacularId(1L), null)

            assertTrue(store.getRecipeSummaries(otherUserId).isEmpty())
        }
}
