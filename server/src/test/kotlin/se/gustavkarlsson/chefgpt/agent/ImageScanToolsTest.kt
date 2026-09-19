package se.gustavkarlsson.chefgpt.agent

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.EventId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.Event
import se.gustavkarlsson.chefgpt.chats.InMemoryEventRepository
import se.gustavkarlsson.chefgpt.ingredients.InMemoryIngredientStore
import se.gustavkarlsson.chefgpt.recipes.InMemoryRecipeStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock

private const val PAGE = "https://res.cloudinary.com/demo/image/upload/v123/page.jpg"
private const val DISH = "https://res.cloudinary.com/demo/image/upload/v123/dish.jpg"
private const val NOTES = "https://res.cloudinary.com/demo/raw/upload/v123/notes.txt"

class ImageScanToolsTest {
    private val chatId = ChatId.random()
    private val userId = UserId.random()
    private val eventRepository = InMemoryEventRepository()
    private val recipeStore = InMemoryRecipeStore()
    private val ingredientStore = InMemoryIngredientStore()
    private val tools =
        ImageScanTools(
            eventRepository,
            chatId,
            userId,
            FakeRecipeScanAgent(recipeStore),
            FakeIngredientScanAgent(ingredientStore),
            FakeDescribeImageAgent(),
        )

    private suspend fun share(vararg attachments: ApiAttachment) {
        eventRepository.append(
            chatId,
            Event.Message(
                id = EventId.random(),
                message = Message.User(listOf(MessagePart.Text("Look")), RequestMetaInfo(Clock.System.now())),
                attachments = attachments.toList(),
            ),
        )
    }

    @Test
    fun `scans recipes from a shared photo`() =
        runTest {
            share(ApiAttachment(PAGE, "image/jpeg", "page.jpg"))

            val result = tools.scanRecipesInPhotos(listOf(PAGE))

            assertEquals("Saved 1 recipe(s): Pasta al pomodoro", result)
            assertEquals(listOf("Pasta al pomodoro"), recipeStore.getRecipeSummaries(userId).map { it.title })
        }

    @Test
    fun `scans recipes from several shared photos`() =
        runTest {
            share(ApiAttachment(PAGE, "image/jpeg", "page.jpg"), ApiAttachment(DISH, "image/jpeg", "dish.jpg"))

            val result = tools.scanRecipesInPhotos(listOf(PAGE, DISH))

            assertEquals("Saved 1 recipe(s): Pasta al pomodoro", result)
        }

    @Test
    fun `scans ingredients from a shared photo`() =
        runTest {
            share(ApiAttachment(PAGE, "image/jpeg", "page.jpg"))

            val result = tools.scanIngredientsInPhotos(listOf(PAGE))

            assertEquals("Found 2 ingredient(s).", result)
            assertEquals(setOf("tomato", "basil"), ingredientStore.getIngredients(userId).map { it.name }.toSet())
        }

    @Test
    fun `describes shared photos`() =
        runTest {
            share(ApiAttachment(PAGE, "image/jpeg", "page.jpg"))

            val result = tools.describePhotos(listOf(PAGE))

            assertEquals("A fake description of the images.", result)
        }

    @Test
    fun `refuses a photo url that was not shared here`() =
        runTest {
            share(ApiAttachment(PAGE, "image/jpeg", "page.jpg"))

            assertFailsWith<IllegalArgumentException> {
                tools.scanRecipesInPhotos(listOf(DISH))
            }
        }

    @Test
    fun `refuses a shared file that is not a photo`() =
        runTest {
            share(ApiAttachment(NOTES, "text/plain", "notes.txt"))

            assertFailsWith<IllegalArgumentException> {
                tools.scanRecipesInPhotos(listOf(NOTES))
            }
        }
}
