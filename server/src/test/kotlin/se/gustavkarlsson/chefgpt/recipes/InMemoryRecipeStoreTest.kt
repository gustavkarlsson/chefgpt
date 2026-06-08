package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryRecipeStoreTest {
    private val userId = UserId.random()
    private val otherUserId = UserId.random()
    private val store = InMemoryRecipeStore()

    @Test
    fun `getRecipes returns empty list for new user`() =
        runTest {
            assertTrue(store.getRecipes(userId).isEmpty())
        }

    @Test
    fun `addRecipe returns the stored recipe`() =
        runTest {
            val added = store.addRecipe(userId, "Carbonara", "https://example.com/carbonara", "https://img/c.jpg")

            assertEquals("Carbonara", added.title)
            assertEquals("https://example.com/carbonara", added.url)
            assertEquals("https://img/c.jpg", added.imageUrl)
        }

    @Test
    fun `addRecipe allows a null image`() =
        runTest {
            val added = store.addRecipe(userId, "Carbonara", "https://example.com/carbonara", null)

            assertNull(added.imageUrl)
        }

    @Test
    fun `addRecipe persists the recipe`() =
        runTest {
            val added = store.addRecipe(userId, "Carbonara", "https://example.com/carbonara", null)

            assertEquals(listOf(added), store.getRecipes(userId))
        }

    @Test
    fun `addRecipe assigns a unique id per recipe`() =
        runTest {
            val first = store.addRecipe(userId, "Carbonara", "https://example.com/a", null)
            val second = store.addRecipe(userId, "Carbonara", "https://example.com/a", null)

            assertTrue(first.id != second.id)
        }

    @Test
    fun `deleteRecipe removes the recipe and returns true`() =
        runTest {
            val added = store.addRecipe(userId, "Carbonara", "https://example.com/carbonara", null)

            val deleted = store.deleteRecipe(userId, added.id)

            assertTrue(deleted)
            assertTrue(store.getRecipes(userId).isEmpty())
        }

    @Test
    fun `deleteRecipe returns false when the recipe does not exist`() =
        runTest {
            assertFalse(store.deleteRecipe(userId, RecipeId.random()))
        }

    @Test
    fun `streamRecipes emits empty list for new user`() =
        runTest {
            assertTrue(store.streamRecipes(userId).first().isEmpty())
        }

    @Test
    fun `streamRecipes emits current recipes`() =
        runTest {
            val added = store.addRecipe(userId, "Carbonara", "https://example.com/carbonara", null)

            assertEquals(listOf(added), store.streamRecipes(userId).first())
        }

    @Test
    fun `recipe store is independent per user`() =
        runTest {
            store.addRecipe(userId, "Carbonara", "https://example.com/carbonara", null)

            assertTrue(store.getRecipes(otherUserId).isEmpty())
        }
}
