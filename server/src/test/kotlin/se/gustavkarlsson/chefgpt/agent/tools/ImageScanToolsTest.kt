package se.gustavkarlsson.chefgpt.agent.tools

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.agent.describeimages.FakeDescribeImagesAgent
import se.gustavkarlsson.chefgpt.agent.saverecipes.FakeSaveRecipesAgent
import se.gustavkarlsson.chefgpt.agent.scaningredients.FakeScanIngredientsAgent
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.ingredients.InMemoryIngredientStore
import se.gustavkarlsson.chefgpt.recipes.InMemoryRecipePersistence
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import kotlin.test.Test
import kotlin.test.assertEquals

private val PAGE = UploadedFile("https://res.cloudinary.com/demo/image/upload/v123/page.jpg", "image/jpeg", "page.jpg")
private val DISH = UploadedFile("https://res.cloudinary.com/demo/image/upload/v123/dish.jpg", "image/jpeg", "dish.jpg")

class ImageScanToolsTest {
    private val userId = UserId.random()
    private val recipeRepository = RecipeRepository(InMemoryRecipePersistence())
    private val ingredientStore = InMemoryIngredientStore()
    private val addRecipes = AddRecipesFromPhotosTool(FakeSaveRecipesAgent(recipeRepository), userId)
    private val addIngredients = AddIngredientsFromPhotosTool(FakeScanIngredientsAgent(), ingredientStore, userId)
    private val describePhotos = DescribePhotosTool(FakeDescribeImagesAgent())

    @Test
    fun `scans recipes from a photo`() =
        runTest {
            val result = addRecipes.addRecipesFromPhotos(listOf(PAGE))

            assertEquals(listOf("Pasta al pomodoro"), recipeRepository.getRecipeSummaries(userId).map { it.title })
            assertEquals(recipeRepository.getRecipeSummaries(userId).map { it.id }, result)
        }

    @Test
    fun `scans recipes from several photos`() =
        runTest {
            val result = addRecipes.addRecipesFromPhotos(listOf(PAGE, DISH))

            assertEquals(recipeRepository.getRecipeSummaries(userId).map { it.id }, result)
        }

    @Test
    fun `scans ingredients from a photo`() =
        runTest {
            val result = addIngredients.addIngredientsFromPhotos(listOf(PAGE))

            assertEquals(listOf("tomato", "basil"), result)
            assertEquals(setOf("tomato", "basil"), ingredientStore.getIngredients(userId).map { it.name }.toSet())
        }

    @Test
    fun `describes photos`() =
        runTest {
            val result = describePhotos.describePhotos(listOf(PAGE))

            assertEquals(listOf("A fake description of an image", "Another description"), result)
        }
}
