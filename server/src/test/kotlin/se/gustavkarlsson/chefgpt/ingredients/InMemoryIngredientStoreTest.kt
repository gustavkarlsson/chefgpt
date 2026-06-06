package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryIngredientStoreTest {
    private val userId = UserId.random()
    private val otherUserId = UserId.random()
    private val store = InMemoryIngredientStore()

    private val List<ApiIngredient>.names get() = map { it.name }

    @Test
    fun `getIngredients returns empty list for new user`() =
        runTest {
            val ingredients = store.getIngredients(userId)

            assertTrue(ingredients.isEmpty())
        }

    @Test
    fun `addIngredients returns the added ingredients as in inventory`() =
        runTest {
            val added = store.addIngredients(userId, listOf("tomato"))

            assertEquals(listOf("tomato"), added.names)
            assertTrue(added.all { it.inInventory })
        }

    @Test
    fun `addIngredients normalizes ingredient names to lowercase`() =
        runTest {
            store.addIngredients(userId, listOf("Tomato"))

            assertEquals(listOf("tomato"), store.getIngredients(userId).names)
        }

    @Test
    fun `addIngredients trims whitespace from ingredient names`() =
        runTest {
            store.addIngredients(userId, listOf("  tomato  "))

            assertEquals(listOf("tomato"), store.getIngredients(userId).names)
        }

    @Test
    fun `addIngredients returns empty list when ingredient already in inventory`() =
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

            assertEquals(listOf("pepper"), added.names)
        }

    @Test
    fun `addIngredients with duplicates in the same call adds the ingredient only once`() =
        runTest {
            val added = store.addIngredients(userId, listOf("tomato", "tomato"))

            assertEquals(listOf("tomato"), added.names)
        }

    @Test
    fun `addIngredients persists ingredients`() =
        runTest {
            store.addIngredients(userId, listOf("tomato", "pepper"))

            assertEquals(setOf("tomato", "pepper"), store.getIngredients(userId).names.toSet())
        }

    @Test
    fun `addIngredients restores an ingredient that was removed from inventory`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))
            store.removeIngredients(userId, listOf("tomato"))

            val added = store.addIngredients(userId, listOf("tomato"))

            assertEquals(listOf("tomato"), added.names)
            assertTrue(store.getIngredients(userId).single().inInventory)
        }

    @Test
    fun `removeIngredients returns the removed ingredients as out of inventory`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))

            val removed = store.removeIngredients(userId, listOf("tomato"))

            assertEquals(listOf("tomato"), removed.names)
            assertTrue(removed.none { it.inInventory })
        }

    @Test
    fun `removeIngredients keeps the ingredient in the store but out of inventory`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))
            store.removeIngredients(userId, listOf("tomato"))

            val ingredient = store.getIngredients(userId).single()

            assertEquals("tomato", ingredient.name)
            assertTrue(!ingredient.inInventory)
        }

    @Test
    fun `removeIngredients returns empty list when ingredient does not exist`() =
        runTest {
            val removed = store.removeIngredients(userId, listOf("tomato"))

            assertTrue(removed.isEmpty())
        }

    @Test
    fun `removeIngredients returns empty list when ingredient already out of inventory`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))
            store.removeIngredients(userId, listOf("tomato"))

            val removed = store.removeIngredients(userId, listOf("tomato"))

            assertTrue(removed.isEmpty())
        }

    @Test
    fun `removeIngredients only affects specified ingredients`() =
        runTest {
            store.addIngredients(userId, listOf("tomato", "pepper"))
            store.removeIngredients(userId, listOf("tomato"))

            val inInventory = store.getIngredients(userId).filter { it.inInventory }

            assertEquals(listOf("pepper"), inInventory.names)
        }

    @Test
    fun `destroyIngredients removes the ingredient from storage`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))
            store.destroyIngredients(userId, listOf("tomato"))

            assertTrue(store.getIngredients(userId).isEmpty())
        }

    @Test
    fun `destroyIngredients returns the destroyed ingredients`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))

            val destroyed = store.destroyIngredients(userId, listOf("tomato"))

            assertEquals(listOf("tomato"), destroyed.names)
        }

    @Test
    fun `destroyIngredients returns empty list when ingredient does not exist`() =
        runTest {
            val destroyed = store.destroyIngredients(userId, listOf("tomato"))

            assertTrue(destroyed.isEmpty())
        }

    @Test
    fun `destroyIngredients removes ingredients even when out of inventory`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))
            store.removeIngredients(userId, listOf("tomato"))

            val destroyed = store.destroyIngredients(userId, listOf("tomato"))

            assertEquals(listOf("tomato"), destroyed.names)
            assertTrue(store.getIngredients(userId).isEmpty())
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

            assertEquals(setOf("tomato", "pepper"), ingredients.names.toSet())
        }

    @Test
    fun `ingredient store is independent per user`() =
        runTest {
            store.addIngredients(userId, listOf("tomato"))

            val otherIngredients = store.getIngredients(otherUserId)

            assertTrue(otherIngredients.isEmpty())
        }
}
