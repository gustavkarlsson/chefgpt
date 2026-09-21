package se.gustavkarlsson.chefgpt.agent

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.agent.describeimage.FakeDescribeImagesAgent
import se.gustavkarlsson.chefgpt.agent.scaningredients.FakeScanIngredientsAgent
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.ingredients.InMemoryIngredientStore
import se.gustavkarlsson.chefgpt.recipes.InMemoryRecipePersistence
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import kotlin.test.Test
import kotlin.test.assertEquals

private val PAGE = UploadedFile("https://res.cloudinary.com/demo/image/upload/v123/page.jpg", "image/jpeg", "page.jpg")
private val DISH = UploadedFile("https://res.cloudinary.com/demo/image/upload/v123/dish.jpg", "image/jpeg", "dish.jpg")

class ImageScanToolsTest {
    private val chatId = ChatId.random()
    private val userId = UserId.random()
    private val eventRepository = InMemoryEventRepository()
    private val recipeRepository = RecipeRepository(InMemoryRecipePersistence())
    private val ingredientStore = InMemoryIngredientStore()
    private val tools =
        ImageScanTools(
            eventRepository,
            chatId,
            userId,
            FakeRecipeScanAgent(recipeRepository),
            FakeScanIngredientsAgent(ingredientStore),
            FakeDescribeImagesAgent(),
        )

    @Test
    fun `scans recipes from a photo`() =
        runTest {
            val result = tools.scanRecipesInPhotos(listOf(PAGE))

            assertEquals(listOf("Pasta al pomodoro"), result)
            assertEquals(listOf("Pasta al pomodoro"), recipeRepository.getRecipeSummaries(userId).map { it.title })
        }

    @Test
    fun `scans recipes from several photos`() =
        runTest {
            val result = tools.scanRecipesInPhotos(listOf(PAGE, DISH))

            assertEquals(listOf("Pasta al pomodoro"), result)
        }

    @Test
    fun `scans ingredients from a photo`() =
        runTest {
            val result = tools.scanIngredientsInPhotos(listOf(PAGE))

            assertEquals(listOf("tomato", "basil"), result)
            assertEquals(setOf("tomato", "basil"), ingredientStore.getIngredients(userId).map { it.name }.toSet())
        }

    @Test
    fun `describes photos`() =
        runTest {
            val result = tools.describePhotos(listOf(PAGE))

            assertEquals(listOf("A fake description of an image", "Another description"), result)
        }
}
