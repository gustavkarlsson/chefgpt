package se.gustavkarlsson.chefgpt.agent.describeimage

import ai.koog.prompt.message.AttachmentSource
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.files.UploadedFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun imageFile(name: String) = UploadedFile("https://example.com/$name", "image/jpeg", name)

private fun pdfFile(name: String) = UploadedFile("https://example.com/$name", "application/pdf", name)

private class FakeDescribeImages(
    private val descriptions: Map<Int, String>,
) : DescribeImages {
    var received: List<AttachmentSource.Image> = emptyList()
        private set

    override suspend fun invoke(images: List<AttachmentSource.Image>): Map<Int, String> {
        received = images
        return descriptions
    }
}

class KoogDescribeImagesAgentTest {
    @Test
    fun `returns one description per file in input order`() =
        runTest {
            val fake = FakeDescribeImages(mapOf(0 to "first", 1 to "second", 2 to "third"))
            val agent = KoogDescribeImagesAgent(fake)
            val files = listOf(imageFile("a.jpg"), pdfFile("b.pdf"), imageFile("c.jpg"), imageFile("d.webp"))

            val result = agent.scan(files)

            assertEquals(listOf("first", "Not an image", "second", "third"), result)
        }

    @Test
    fun `assigns indices by image position ignoring preceding non-images`() =
        runTest {
            val fake = FakeDescribeImages(mapOf(0 to "first", 1 to "second"))
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.scan(listOf(pdfFile("a.pdf"), imageFile("b.jpg"), imageFile("c.jpg")))

            assertEquals(listOf("Not an image", "first", "second"), result)
        }

    @Test
    fun `passes only image files to the describer in order`() =
        runTest {
            val fake = FakeDescribeImages(emptyMap())
            val agent = KoogDescribeImagesAgent(fake)

            agent.scan(listOf(imageFile("a.jpg"), pdfFile("b.pdf"), imageFile("c.jpg")))

            assertEquals(listOf("a.jpg", "c.jpg"), fake.received.map { it.fileName })
        }

    @Test
    fun `returns Not an image for every file when none are images`() =
        runTest {
            val fake = FakeDescribeImages(emptyMap())
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.scan(listOf(pdfFile("a.pdf"), pdfFile("b.pdf")))

            assertEquals(listOf("Not an image", "Not an image"), result)
            assertTrue(fake.received.isEmpty())
        }

    @Test
    fun `returns empty list when there are no files`() =
        runTest {
            val agent = KoogDescribeImagesAgent(FakeDescribeImages(emptyMap()))

            val result = agent.scan(emptyList())

            assertTrue(result.isEmpty())
        }

    @Test
    fun `marks image as skipped when its index has no description`() =
        runTest {
            val fake = FakeDescribeImages(mapOf(0 to "first"))
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.scan(listOf(imageFile("a.jpg"), imageFile("b.jpg")))

            assertEquals(listOf("first", "Skipped by agent"), result)
        }
}
