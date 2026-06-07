package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryIngredientStoreTest {
    private val userId = UserId.random()
    private val otherUserId = UserId.random()
    private val store = InMemoryIngredientStore()

    private val List<ApiIngredient>.names get() = map { it.name }

    private suspend fun ids(vararg names: String): List<IngredientId> {
        val wanted = names.toSet()
        return store.getIngredients(userId).filter { it.name in wanted }.map { it.id }
    }

    @Test
    fun `getIngredients returns empty list for new user`() =
        runTest {
            val ingredients = store.getIngredients(userId)

            assertTrue(ingredients.isEmpty())
        }

    @Test
    fun `createIngredients returns the added ingredients as in inventory`() =
        runTest {
            val added = store.createIngredients(userId, listOf("tomato"))

            assertEquals(listOf("tomato"), added.names)
            assertTrue(added.all { it.inInventory })
        }

    @Test
    fun `createIngredients normalizes ingredient names to lowercase`() =
        runTest {
            store.createIngredients(userId, listOf("Tomato"))

            assertEquals(listOf("tomato"), store.getIngredients(userId).names)
        }

    @Test
    fun `createIngredients trims whitespace from ingredient names`() =
        runTest {
            store.createIngredients(userId, listOf("  tomato  "))

            assertEquals(listOf("tomato"), store.getIngredients(userId).names)
        }

    @Test
    fun `createIngredients returns empty list when ingredient already in inventory`() =
        runTest {
            store.createIngredients(userId, listOf("tomato"))

            val added = store.createIngredients(userId, listOf("tomato"))

            assertTrue(added.isEmpty())
        }

    @Test
    fun `createIngredients returns only newly added ingredients`() =
        runTest {
            store.createIngredients(userId, listOf("tomato"))

            val added = store.createIngredients(userId, listOf("tomato", "pepper"))

            assertEquals(listOf("pepper"), added.names)
        }

    @Test
    fun `createIngredients with duplicates in the same call adds the ingredient only once`() =
        runTest {
            val added = store.createIngredients(userId, listOf("tomato", "tomato"))

            assertEquals(listOf("tomato"), added.names)
        }

    @Test
    fun `createIngredients persists ingredients`() =
        runTest {
            store.createIngredients(userId, listOf("tomato", "pepper"))

            assertEquals(setOf("tomato", "pepper"), store.getIngredients(userId).names.toSet())
        }

    @Test
    fun `createIngredients keeps the same id when restoring a removed ingredient`() =
        runTest {
            val added = store.createIngredients(userId, listOf("tomato")).single()
            store.setInventory(userId, listOf(added.id), inInventory = false)

            val restored = store.createIngredients(userId, listOf("tomato")).single()

            assertEquals(added.id, restored.id)
            assertTrue(store.getIngredients(userId).single().inInventory)
        }

    @Test
    fun `setInventory false takes the ingredient out of inventory but keeps it in the store`() =
        runTest {
            val tomatoId = store.createIngredients(userId, listOf("tomato")).single().id

            val updated = store.setInventory(userId, listOf(tomatoId), inInventory = false).single()

            assertEquals("tomato", updated.name)
            assertEquals(false, updated.inInventory)
            assertEquals(listOf("tomato"), store.getIngredients(userId).names)
            assertTrue(store.getIngredients(userId).none { it.inInventory })
        }

    @Test
    fun `setInventory true puts a removed ingredient back into inventory`() =
        runTest {
            val tomatoId = store.createIngredients(userId, listOf("tomato")).single().id
            store.setInventory(userId, listOf(tomatoId), inInventory = false)

            val updated = store.setInventory(userId, listOf(tomatoId), inInventory = true).single()

            assertEquals(tomatoId, updated.id)
            assertEquals(true, updated.inInventory)
        }

    @Test
    fun `setInventory applies the same value to multiple ingredients`() =
        runTest {
            val ids = store.createIngredients(userId, listOf("tomato", "basil", "pepper")).map { it.id }

            val updated = store.setInventory(userId, ids, inInventory = false)

            assertEquals(setOf("tomato", "basil", "pepper"), updated.names.toSet())
            assertTrue(updated.none { it.inInventory })
            assertTrue(store.getIngredients(userId).none { it.inInventory })
        }

    @Test
    fun `setInventory skips ids that do not exist`() =
        runTest {
            val tomatoId = store.createIngredients(userId, listOf("tomato")).single().id

            val updated = store.setInventory(userId, listOf(tomatoId, IngredientId.random()), inInventory = false)

            assertEquals(listOf("tomato"), updated.names)
        }

    @Test
    fun `setInventory returns empty list when no ingredient exists`() =
        runTest {
            val updated = store.setInventory(userId, listOf(IngredientId.random()), inInventory = true)

            assertTrue(updated.isEmpty())
        }

    @Test
    fun `destroyIngredients removes the ingredient from storage`() =
        runTest {
            store.createIngredients(userId, listOf("tomato"))
            store.destroyIngredients(userId, ids("tomato"))

            assertTrue(store.getIngredients(userId).isEmpty())
        }

    @Test
    fun `destroyIngredients returns the destroyed ingredients`() =
        runTest {
            store.createIngredients(userId, listOf("tomato"))

            val destroyed = store.destroyIngredients(userId, ids("tomato"))

            assertEquals(listOf("tomato"), destroyed.names)
        }

    @Test
    fun `destroyIngredients returns empty list when ingredient does not exist`() =
        runTest {
            val destroyed = store.destroyIngredients(userId, listOf(IngredientId.random()))

            assertTrue(destroyed.isEmpty())
        }

    @Test
    fun `destroyIngredients removes ingredients even when out of inventory`() =
        runTest {
            val tomatoId = store.createIngredients(userId, listOf("tomato")).single().id
            store.setInventory(userId, listOf(tomatoId), inInventory = false)

            val destroyed = store.destroyIngredients(userId, ids("tomato"))

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
            store.createIngredients(userId, listOf("tomato", "pepper"))

            val ingredients = store.streamIngredients(userId).first()

            assertEquals(setOf("tomato", "pepper"), ingredients.names.toSet())
        }

    @Test
    fun `ingredient store is independent per user`() =
        runTest {
            store.createIngredients(userId, listOf("tomato"))

            val otherIngredients = store.getIngredients(otherUserId)

            assertTrue(otherIngredients.isEmpty())
        }
}
