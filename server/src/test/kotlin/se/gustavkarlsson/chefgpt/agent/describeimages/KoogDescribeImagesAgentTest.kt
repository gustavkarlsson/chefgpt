package se.gustavkarlsson.chefgpt.agent.describeimages

import ai.koog.prompt.message.AttachmentSource
import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.files.UploadedFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun imageFile(name: String) = UploadedFile("https://example.com/$name", "image/jpeg", name)

private fun pdfFile(name: String) = UploadedFile("https://example.com/$name", "application/pdf", name)

private class FakeDescribeImages(
    private val text: String,
) : DescribeImages {
    var received: List<AttachmentSource.Image> = emptyList()
        private set

    override suspend fun invoke(images: List<AttachmentSource.Image>): String {
        received = images
        return text
    }
}

class KoogDescribeImagesAgentTest {
    @Test
    fun `returns one description per file in input order`() =
        runTest {
            val fake = FakeDescribeImages("[0] first\n[1] second\n[2] third")
            val agent = KoogDescribeImagesAgent(fake)
            val files = listOf(imageFile("a.jpg"), pdfFile("b.pdf"), imageFile("c.jpg"), imageFile("d.webp"))

            val result = agent.run(files)

            assertEquals(listOf("first", "Not an image", "second", "third"), result)
        }

    @Test
    fun `assigns indices by image position ignoring preceding non-images`() =
        runTest {
            val fake = FakeDescribeImages("[0] first\n[1] second")
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(listOf(pdfFile("a.pdf"), imageFile("b.jpg"), imageFile("c.jpg")))

            assertEquals(listOf("Not an image", "first", "second"), result)
        }

    @Test
    fun `passes only image files to the describer in order`() =
        runTest {
            val fake = FakeDescribeImages("")
            val agent = KoogDescribeImagesAgent(fake)

            agent.run(listOf(imageFile("a.jpg"), pdfFile("b.pdf"), imageFile("c.jpg")))

            assertEquals(listOf("a.jpg", "c.jpg"), fake.received.map { it.fileName })
        }

    @Test
    fun `returns Not an image for every file when none are images`() =
        runTest {
            val fake = FakeDescribeImages("")
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(listOf(pdfFile("a.pdf"), pdfFile("b.pdf")))

            assertEquals(listOf("Not an image", "Not an image"), result)
            assertTrue(fake.received.isEmpty())
        }

    @Test
    fun `returns empty list when there are no files`() =
        runTest {
            val agent = KoogDescribeImagesAgent(FakeDescribeImages(""))

            val result = agent.run(emptyList())

            assertTrue(result.isEmpty())
        }

    @Test
    fun `marks image as skipped when its index has no description`() =
        runTest {
            val fake = FakeDescribeImages("[0] first")
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(listOf(imageFile("a.jpg"), imageFile("b.jpg")))

            assertEquals(listOf("first", "Skipped by agent"), result)
        }

    @Test
    fun `marks all images as skipped when the describer returns empty text`() =
        runTest {
            val fake = FakeDescribeImages("")
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(listOf(imageFile("a.jpg"), imageFile("b.jpg")))

            assertEquals(listOf("Skipped by agent", "Skipped by agent"), result)
        }

    @Test
    fun `parses descriptions in any index order`() =
        runTest {
            val fake = FakeDescribeImages("[2] third\n[0] first\n[1] second")
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(listOf(imageFile("a.jpg"), imageFile("b.jpg"), imageFile("c.jpg")))

            assertEquals(listOf("first", "second", "third"), result)
        }

    @Test
    fun `shifts off-by-one indices to match the images`() =
        runTest {
            val fake = FakeDescribeImages("[1] first\n[2] second\n[3] third")
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(listOf(imageFile("a.jpg"), imageFile("b.jpg"), imageFile("c.jpg")))

            assertEquals(listOf("first", "second", "third"), result)
        }

    @Test
    fun `ignores lines that do not match the description format`() =
        runTest {
            val fake = FakeDescribeImages("Here are the descriptions:\n[0] first\n\nThanks!")
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(listOf(imageFile("a.jpg")))

            assertEquals(listOf("first"), result)
        }

    @Test
    fun `keeps the last description when an index repeats`() =
        runTest {
            val fake = FakeDescribeImages("[0] first\n[0] second")
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(listOf(imageFile("a.jpg")))

            assertEquals(listOf("second"), result)
        }

    @Test
    fun `ignores descriptions for indices beyond the images`() =
        runTest {
            val fake = FakeDescribeImages("[0] first\n[1] second\n[2] third")
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(listOf(imageFile("a.jpg"), imageFile("b.jpg")))

            assertEquals(listOf("first", "second"), result)
        }

    @Test
    fun `trims whitespace from descriptions`() =
        runTest {
            val fake = FakeDescribeImages("[0]   first   ")
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(listOf(imageFile("a.jpg")))

            assertEquals(listOf("first"), result)
        }

    @Test
    fun `parses multi-digit indices`() =
        runTest {
            val files = (0..10).map { imageFile("img$it.jpg") }
            val fake = FakeDescribeImages((0..10).joinToString("\n") { "[$it] desc $it" })
            val agent = KoogDescribeImagesAgent(fake)

            val result = agent.run(files)

            assertEquals((0..10).map { "desc $it" }, result)
        }
}
