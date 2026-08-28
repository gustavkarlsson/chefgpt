package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.api.toSummary
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class InMemoryRecipeStoreTest {
    private val userId = UserId.random()
    private val otherUserId = UserId.random()
    private val store = InMemoryRecipeStore()

    @Test
    fun `getRecipeSummaries returns empty list for new user`() =
        runTest {
            assertTrue(store.getRecipeSummaries(userId).isEmpty())
        }

    @Test
    fun `saveRecipe returns the stored recipe`() =
        runTest {
            val recipe = carbonara()

            val saved = store.saveRecipe(userId, recipe)

            assertEquals(recipe.toApiRecipe(saved.id, favorite = false), saved)
        }

    @Test
    fun `saveRecipe stores the recipe as a non-favorite`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            assertFalse(saved.favorite)
        }

    @Test
    fun `getRecipe returns the full recipe`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            assertEquals(saved, store.getRecipe(userId, saved.id))
        }

    @Test
    fun `getRecipe returns null for another user's recipe`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            assertNull(store.getRecipe(otherUserId, saved.id))
        }

    @Test
    fun `getRecipeSummaries returns a summary of each stored recipe`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            assertEquals(listOf(saved.toSummary()), store.getRecipeSummaries(userId))
        }

    @Test
    fun `saveRecipe assigns a unique id per recipe`() =
        runTest {
            val first = store.saveRecipe(userId, carbonara())

            val second = store.saveRecipe(userId, carbonara())

            assertTrue(first.id != second.id)
        }

    @Test
    fun `modifyRecipe only changes the parts given`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            assertEquals(
                saved.copy(id = modified!!.id, title = "Vegetarian carbonara", modifiedFrom = saved.id),
                modified,
            )
        }

    @Test
    fun `modifyRecipe replaces the servings range`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(servings = 6..8))

            assertEquals(6..8, modified?.servings)
        }

    @Test
    fun `modifyRecipe replaces the whole list of steps`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(steps = listOf("Order takeout")))

            assertEquals(listOf("Order takeout"), modified?.steps)
        }

    @Test
    fun `modifyRecipe leaves the modified recipe untouched`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            store.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            assertEquals(saved, store.getRecipe(userId, saved.id))
        }

    @Test
    fun `modifyRecipe keeps the favorite state`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())
            store.setFavorite(userId, saved.id, favorite = true)

            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Carbonara for one"))

            assertTrue(modified?.favorite == true)
        }

    @Test
    fun `modifyRecipe updates an existing modification in place`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())
            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            val remodified = store.modifyRecipe(userId, modified!!.id, RecipeUpdate(title = "Vegan carbonara"))

            assertEquals(modified.copy(title = "Vegan carbonara"), remodified)
        }

    @Test
    fun `modifyRecipe reuses the existing modification of a recipe`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())
            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            val remodified = store.modifyRecipe(userId, saved.id, RecipeUpdate(description = "Now meat free"))

            assertEquals(modified?.id, remodified?.id)
        }

    @Test
    fun `modifyRecipe returns null when the recipe does not exist`() =
        runTest {
            assertNull(store.modifyRecipe(userId, RecipeId.random(), RecipeUpdate(title = "Nothing")))
        }

    @Test
    fun `modifyRecipe returns null for another user's recipe`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            assertNull(store.modifyRecipe(otherUserId, saved.id, RecipeUpdate(title = "Stolen carbonara")))
        }

    @Test
    fun `summaries represent a modified recipe by its modification`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())
            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            val summaries = store.getRecipeSummaries(userId)

            assertEquals(listOf(modified?.toSummary()), summaries)
        }

    @Test
    fun `overwriteOriginal deletes the recipe the modification came from`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())
            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            store.overwriteOriginal(userId, modified!!.id)

            assertNull(store.getRecipe(userId, saved.id))
        }

    @Test
    fun `overwriteOriginal keeps the modification as an ordinary recipe`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())
            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            val overwritten = store.overwriteOriginal(userId, modified!!.id)

            assertEquals(modified.copy(modifiedFrom = null), overwritten)
        }

    @Test
    fun `overwriteOriginal returns null for a recipe that is not a modification`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            assertNull(store.overwriteOriginal(userId, saved.id))
        }

    @Test
    fun `overwriteOriginal returns null when the recipe does not exist`() =
        runTest {
            assertNull(store.overwriteOriginal(userId, RecipeId.random()))
        }

    @Test
    fun `saveAsCopy keeps both recipes`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())
            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            val copy = store.saveAsCopy(userId, modified!!.id)

            assertEquals(
                listOf(saved.toSummary(), modified.copy(modifiedFrom = null).toSummary()),
                store.getRecipeSummaries(userId),
            )
            assertNull(copy?.modifiedFrom)
        }

    @Test
    fun `saveAsCopy returns null for a recipe that is not a modification`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            assertNull(store.saveAsCopy(userId, saved.id))
        }

    @Test
    fun `discarding a modification brings the recipe back`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())
            val modified = store.modifyRecipe(userId, saved.id, RecipeUpdate(title = "Vegetarian carbonara"))

            store.deleteRecipe(userId, modified!!.id)

            assertEquals(listOf(saved.toSummary()), store.getRecipeSummaries(userId))
        }

    @Test
    fun `setFavorite makes the recipe a favorite`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            val updated = store.setFavorite(userId, saved.id, favorite = true)

            assertTrue(updated?.favorite == true)
        }

    @Test
    fun `setFavorite is reflected in the summaries`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            store.setFavorite(userId, saved.id, favorite = true)

            assertTrue(store.getRecipeSummaries(userId).single().favorite)
        }

    @Test
    fun `setFavorite returns null when the recipe does not exist`() =
        runTest {
            assertNull(store.setFavorite(userId, RecipeId.random(), favorite = true))
        }

    @Test
    fun `deleteRecipe removes the recipe and returns true`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            val deleted = store.deleteRecipe(userId, saved.id)

            assertTrue(deleted)
            assertTrue(store.getRecipeSummaries(userId).isEmpty())
        }

    @Test
    fun `deleteRecipe returns false when the recipe does not exist`() =
        runTest {
            assertFalse(store.deleteRecipe(userId, RecipeId.random()))
        }

    @Test
    fun `streamRecipeSummaries emits empty list for new user`() =
        runTest {
            assertTrue(store.streamRecipeSummaries(userId).first().isEmpty())
        }

    @Test
    fun `streamRecipeSummaries emits current recipe summaries`() =
        runTest {
            val saved = store.saveRecipe(userId, carbonara())

            assertEquals(listOf(saved.toSummary()), store.streamRecipeSummaries(userId).first())
        }

    @Test
    fun `recipe store is independent per user`() =
        runTest {
            store.saveRecipe(userId, carbonara())

            assertTrue(store.getRecipeSummaries(otherUserId).isEmpty())
        }
}

private fun carbonara(spoonacularId: SpoonacularId? = SpoonacularId(1L)) =
    NewRecipe(
        title = "Carbonara",
        steps = listOf("Boil the pasta", "Fry the pancetta"),
        spoonacularId = spoonacularId,
        imageUrl = ImageUrl("https://img/c.jpg"),
        description = "A classic Italian pasta dish.",
        preparationDuration = 10.minutes,
        cookingDuration = 20.minutes,
        duration = 30.minutes,
        servings = 4..4,
        ingredients =
            listOf(
                ApiRecipeIngredient("spaghetti", "400", "g"),
                ApiRecipeIngredient("eggs", "4"),
            ),
        nutrients = listOf(ApiNutrient("Calories", "450", "kcal")),
    )
