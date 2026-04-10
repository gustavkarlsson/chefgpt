package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryIngredientStoreTest {
    private val userId = UserId.random()
    private val otherUserId = UserId.random()
    private val store = InMemoryIngredientStore()

    @Test
    fun `getIngredients returns empty list for new user`() =
        runTest {
            val ingredients = store.getIngredients(userId)

            assertTrue(ingredients.isEmpty())
        }

    @Test
    fun `addIngredients returns the added ingredients`() =
        runTest {
            val added = store.addIngredients(userId, listOf("tomato"))

            assertEquals(listOf("tomato"), added)
        }

    @Test
    fun `addIngredients normalizes ingredient names to lowercase`() =
        runTest {
            store.addIngredients(userId, listOf("Tomato"))

            val ingredients = store.getIngredients(userId)

            assertEquals(listOf("tomato"), ingredients)
        }

    @Test
    fun `addIngredients trims whitespace from ingredient names`() =
        runTest {
            store.addIngredients(userId, listOf("  tomato  "))

            val ingredients = store.getIngredients(userId)

            assertEquals(listOf("tomato"), ingredients)
        }

    @Test
    fun `addIngredients returns empty list when ingredient already exists`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))

            val added = store.addIngredients(userId, listOf("tomato"))

            assertTrue(added.isEmpty())
        }

    @Test
    fun `addIngredients returns only newly added ingredients`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))

            val added = store.addIngredients(userId, listOf("tomato", "pepper"))

            assertEquals(listOf("pepper"), added)
        }

    @Test
    fun `addIngredients with duplicates in the same call adds the ingredient only once`() =
        runTest {
            val added = store.addIngredients(userId, listOf("tomato", "tomato"))

            assertEquals(listOf("tomato"), added)
        }

    @Test
    fun `addIngredients persists ingredients`() =
        runTest {
            store.addIngredients(userId, listOf("tomato", "pepper"))

            val ingredients = store.getIngredients(userId)

            assertEquals(setOf("tomato", "pepper"), ingredients.toSet())
        }

    @Test
    fun `removeIngredients returns the removed ingredients`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))

            val removed = store.removeIngredients(userId, listOf("tomato"))

            assertEquals(listOf("tomato"), removed)
        }

    @Test
    fun `removeIngredients deletes the ingredient from storage`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))
            store.removeIngredients(userId, listOf("tomato"))

            val ingredients = store.getIngredients(userId)

            assertTrue(ingredients.isEmpty())
        }

    @Test
    fun `removeIngredients returns empty list when ingredient does not exist`() =
        runTest {
            val removed = store.removeIngredients(userId, listOf("tomato"))

            assertTrue(removed.isEmpty())
        }

    @Test
    fun `removeIngredients only removes specified ingredients`() =
        runTest {
            store.addIngredients(userId, listOf("tomato", "pepper"))
            store.removeIngredients(userId, listOf("tomato"))

            val ingredients = store.getIngredients(userId)

            assertEquals(listOf("pepper"), ingredients)
        }

    @Test
    fun `clearIngredients removes all stored ingredients`() =
        runTest {
            store.addIngredients(userId, listOf("tomato", "pepper"))
            store.clearIngredients(userId)

            val ingredients = store.getIngredients(userId)

            assertTrue(ingredients.isEmpty())
        }

    @Test
    fun `clearIngredients returns all ingredients that were stored`() =
        runTest {
            store.addIngredients(userId, listOf("tomato", "pepper"))

            val cleared = store.clearIngredients(userId)

            assertEquals(setOf("tomato", "pepper"), cleared.toSet())
        }

    @Test
    fun `clearIngredients returns empty list when nothing is stored`() =
        runTest {
            val cleared = store.clearIngredients(userId)

            assertTrue(cleared.isEmpty())
        }

    @Test
    fun `streamIngredients emits empty list for new user`() =
        runTest {
            val ingredients = store.streamIngredients(userId).first()

            assertTrue(ingredients.isEmpty())
        }

    @Test
    fun `streamIngredients emits current ingredients`() =
        runTest {
            store.addIngredients(userId, listOf("tomato", "pepper"))

            val ingredients = store.streamIngredients(userId).first()

            assertEquals(setOf("tomato", "pepper"), ingredients.toSet())
        }

    @Test
    fun `ingredient store is independent per user`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))

            val otherIngredients = store.getIngredients(otherUserId)

            assertTrue(otherIngredients.isEmpty())
        }
}
