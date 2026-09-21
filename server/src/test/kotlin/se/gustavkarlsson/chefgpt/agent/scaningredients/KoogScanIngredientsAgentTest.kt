package se.gustavkarlsson.chefgpt.agent.scaningredients

import ai.koog.prompt.message.AttachmentSource
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.ingredients.InMemoryIngredientStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun imageFile(name: String) = UploadedFile("https://example.com/$name", "image/jpeg", name)

private fun pdfFile(name: String) = UploadedFile("https://example.com/$name", "application/pdf", name)

private class FakeScanIngredients(
    private val text: String,
) : ScanIngredients {
    var receivedImages: List<AttachmentSource.Image> = emptyList()
        private set
    var receivedExisting: List<String> = emptyList()
        private set

    override suspend fun invoke(
        images: List<AttachmentSource.Image>,
        existingIngredients: List<String>,
    ): String {
        receivedImages = images
        receivedExisting = existingIngredients
        return text
    }
}

class KoogScanIngredientsAgentTest {
    private val userId = UserId.random()

    @Test
    fun `parses one ingredient per line`() =
        runTest {
            val fake = FakeScanIngredients("tomatoes\neggs\nmilk")
            val agent = KoogScanIngredientsAgent(fake, InMemoryIngredientStore())

            val result = agent.scan(userId, listOf(imageFile("a.jpg")))

            assertEquals(listOf("tomatoes", "eggs", "milk"), result)
        }

    @Test
    fun `strips list markers and blank lines`() =
        runTest {
            val fake = FakeScanIngredients("- tomatoes\n\n1. eggs\n\n* milk")
            val agent = KoogScanIngredientsAgent(fake, InMemoryIngredientStore())

            val result = agent.scan(userId, listOf(imageFile("a.jpg")))

            assertEquals(listOf("tomatoes", "eggs", "milk"), result)
        }

    @Test
    fun `uses the existing ingredient's spelling when equal ignoring case`() =
        runTest {
            val store = InMemoryIngredientStore()
            store.createIngredients(userId, listOf("tomato", "black pepper"))
            val fake = FakeScanIngredients("Tomato\nBlack Pepper\nbasil")
            val agent = KoogScanIngredientsAgent(fake, store)

            val result = agent.scan(userId, listOf(imageFile("a.jpg")))

            assertEquals(listOf("tomato", "black pepper", "basil"), result)
        }

    @Test
    fun `keeps the scanned spelling for new ingredients`() =
        runTest {
            val store = InMemoryIngredientStore()
            store.createIngredients(userId, listOf("tomato"))
            val fake = FakeScanIngredients("Basil\nmilk")
            val agent = KoogScanIngredientsAgent(fake, store)

            val result = agent.scan(userId, listOf(imageFile("a.jpg")))

            assertEquals(listOf("Basil", "milk"), result)
        }

    @Test
    fun `removes duplicates`() =
        runTest {
            val fake = FakeScanIngredients("basil\nbasil\nbasil")
            val agent = KoogScanIngredientsAgent(fake, InMemoryIngredientStore())

            val result = agent.scan(userId, listOf(imageFile("a.jpg")))

            assertEquals(listOf("basil"), result)
        }

    @Test
    fun `passes only image files to the scanner in order`() =
        runTest {
            val fake = FakeScanIngredients("")
            val agent = KoogScanIngredientsAgent(fake, InMemoryIngredientStore())

            agent.scan(userId, listOf(imageFile("a.jpg"), pdfFile("b.pdf"), imageFile("c.jpg")))

            assertEquals(listOf("a.jpg", "c.jpg"), fake.receivedImages.map { it.fileName })
        }

    @Test
    fun `passes existing ingredient names to the scanner`() =
        runTest {
            val store = InMemoryIngredientStore()
            store.createIngredients(userId, listOf("Black Pepper"))
            val fake = FakeScanIngredients("")
            val agent = KoogScanIngredientsAgent(fake, store)

            agent.scan(userId, listOf(imageFile("a.jpg")))

            assertEquals(listOf("black pepper"), fake.receivedExisting)
        }

    @Test
    fun `returns empty list when there are no images`() =
        runTest {
            val fake = FakeScanIngredients("tomatoes")
            val agent = KoogScanIngredientsAgent(fake, InMemoryIngredientStore())

            val result = agent.scan(userId, emptyList())

            assertTrue(result.isEmpty())
            assertTrue(fake.receivedImages.isEmpty())
        }

    @Test
    fun `returns empty list when the scanner returns blank text`() =
        runTest {
            val fake = FakeScanIngredients("  \n\n  ")
            val agent = KoogScanIngredientsAgent(fake, InMemoryIngredientStore())

            val result = agent.scan(userId, listOf(imageFile("a.jpg")))

            assertTrue(result.isEmpty())
        }
}
