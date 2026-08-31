package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chefGptJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val UPLOADED_PHOTO = "https://res.cloudinary.com/demo/image/upload/v123/page.jpg"

// The recipe createRecipe is expected to store, with everything the agent didn't give spelled out.
private fun pannkakor(
    id: RecipeId,
    steps: List<String>,
    description: String,
    preparationDuration: Duration,
    ingredients: List<ApiRecipeIngredient>,
) = ApiRecipe(
    id = id,
    spoonacularId = null,
    title = "Pannkakor",
    imageUrl = null,
    steps = steps,
    favorite = false,
    modifiedFrom = null,
    description = description,
    preparationDuration = preparationDuration,
    cookingDuration = null,
    duration = null,
    servings = null,
    ingredients = ingredients,
    nutrients = emptyList(),
)

class RecipeStoreToolsCreateRecipeTest {
    private val userId = UserId.random()
    private val store = InMemoryRecipeStore()
    private val tools =
        RecipeStoreTools(
            store = store,
            lookup = RecipeLookup(FakeRecipeClient(), chefGptJson(strict = true)),
            userId = userId,
        )

    @Test
    fun `stores what was read out of the shared file`() =
        runTest {
            val summary =
                tools.createRecipe(
                    title = "Pannkakor",
                    steps = listOf("Whisk", "Fry"),
                    ingredients = listOf(ApiRecipeIngredient("flour", "3", "dl")),
                    description = "Grandma's pancakes",
                    preparationMinutes = 10,
                )

            assertEquals(
                pannkakor(
                    id = summary.id,
                    steps = listOf("Whisk", "Fry"),
                    description = "Grandma's pancakes",
                    preparationDuration = 10.minutes,
                    ingredients = listOf(ApiRecipeIngredient("flour", "3", "dl")),
                ),
                store.getRecipe(userId, summary.id),
            )
        }

    @Test
    fun `keeps a recipe created this way out of spoonacular`() =
        runTest {
            val summary = tools.createRecipe(title = "Pannkakor", steps = listOf("Whisk"))

            assertNull(store.getRecipe(userId, summary.id)?.spoonacularId)
        }

    @Test
    fun `uses the photo url the agent gave`() =
        runTest {
            val summary = tools.createRecipe(title = "Pannkakor", steps = listOf("Whisk"), imageUrl = UPLOADED_PHOTO)

            assertEquals(ImageUrl(UPLOADED_PHOTO), store.getRecipe(userId, summary.id)?.imageUrl)
        }

    @Test
    fun `uses a cropped photo url the agent gave`() =
        runTest {
            val cropped =
                "https://res.cloudinary.com/demo/image/upload/" +
                    "c_crop,x_0.0000,y_0.0000,w_0.9999,h_0.3300/v123/page.jpg"

            val summary = tools.createRecipe(title = "Pannkakor", steps = listOf("Whisk"), imageUrl = cropped)

            assertEquals(ImageUrl(cropped), store.getRecipe(userId, summary.id)?.imageUrl)
        }

    @Test
    fun `drops a photo url pointing somewhere we did not put it`() =
        runTest {
            val summary =
                tools.createRecipe(
                    title = "Pannkakor",
                    steps = listOf("Whisk"),
                    imageUrl = "https://example.com/page.jpg",
                )

            assertNull(store.getRecipe(userId, summary.id)?.imageUrl)
        }

    @Test
    fun `leaves out servings when neither bound is given`() =
        runTest {
            val summary = tools.createRecipe(title = "Pannkakor", steps = listOf("Whisk"))

            assertNull(store.getRecipe(userId, summary.id)?.servings)
        }

    @Test
    fun `leaves out servings when only the lower bound is given`() =
        runTest {
            val summary = tools.createRecipe(title = "Pannkakor", steps = listOf("Whisk"), minServings = 4)

            assertNull(store.getRecipe(userId, summary.id)?.servings)
        }

    @Test
    fun `leaves out servings when only the upper bound is given`() =
        runTest {
            val summary = tools.createRecipe(title = "Pannkakor", steps = listOf("Whisk"), maxServings = 6)

            assertNull(store.getRecipe(userId, summary.id)?.servings)
        }

    @Test
    fun `stores an exact yield as a range with equal bounds`() =
        runTest {
            val summary =
                tools.createRecipe(
                    title = "Pannkakor",
                    steps = listOf("Whisk"),
                    minServings = 4,
                    maxServings = 4,
                )

            assertEquals(4..4, store.getRecipe(userId, summary.id)?.servings)
        }

    @Test
    fun `stores both bounds when the recipe gives a range of servings`() =
        runTest {
            val summary =
                tools.createRecipe(
                    title = "Pannkakor",
                    steps = listOf("Whisk"),
                    minServings = 4,
                    maxServings = 6,
                )

            assertEquals(4..6, store.getRecipe(userId, summary.id)?.servings)
        }

    @Test
    fun `orders swapped servings bounds`() =
        runTest {
            val summary =
                tools.createRecipe(
                    title = "Pannkakor",
                    steps = listOf("Whisk"),
                    minServings = 6,
                    maxServings = 4,
                )

            assertEquals(4..6, store.getRecipe(userId, summary.id)?.servings)
        }

    @Test
    fun `refuses a recipe without steps`() =
        runTest {
            assertFailsWith<IllegalArgumentException> {
                tools.createRecipe(title = "Pannkakor", steps = emptyList())
            }
        }
}
