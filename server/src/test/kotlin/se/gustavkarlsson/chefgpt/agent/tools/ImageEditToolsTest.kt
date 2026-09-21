package se.gustavkarlsson.chefgpt.agent.tools

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.files.CloudinaryImageCropper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val PAGE = "https://res.cloudinary.com/demo/image/upload/v123/page.jpg"
private const val DISH = "https://res.cloudinary.com/demo/image/upload/v123/dish.jpg"

class ImageEditToolsTest {
    private val tools = CropImageTool(CloudinaryImageCropper("demo")) { listOf(PAGE) }

    @Test
    fun `crops a picture`() =
        runTest {
            assertEquals(
                "https://res.cloudinary.com/demo/image/upload/" +
                    "c_crop,x_0.0000,y_0.0000,w_0.9999,h_0.3300/v123/page.jpg",
                tools.cropImage(PAGE, x = 0.0, y = 0.0, width = 1.0, height = 0.33),
            )
        }

    @Test
    fun `refuses to crop a picture that is not available`() =
        runTest {
            assertFailsWith<IllegalArgumentException> {
                tools.cropImage(DISH, x = 0.0, y = 0.0, width = 0.5, height = 0.5)
            }
        }

    @Test
    fun `says so when the region is not inside the picture`() =
        runTest {
            assertFailsWith<IllegalStateException> {
                tools.cropImage(PAGE, x = 0.8, y = 0.0, width = 0.5, height = 0.5)
            }
        }
}
