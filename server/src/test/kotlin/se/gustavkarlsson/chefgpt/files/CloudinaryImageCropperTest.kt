package se.gustavkarlsson.chefgpt.files

import se.gustavkarlsson.chefgpt.api.ImageUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class CloudinaryImageCropperTest {
    private val cropper = CloudinaryImageCropper("demo")

    @Test
    fun `inserts transformation after upload segment`() {
        val imageUrl = ImageUrl("https://res.cloudinary.com/demo/image/upload/v123/page.jpg")

        val cropped = cropper.crop(imageUrl, CropRegion(x = 0.25, y = 0.5, width = 0.5, height = 0.25))

        assertEquals(
            ImageUrl(
                "https://res.cloudinary.com/demo/image/upload/" +
                    "c_crop,x_0.2500,y_0.5000,w_0.5000,h_0.2500/v123/page.jpg",
            ),
            cropped,
        )
    }

    @Test
    fun `keeps fractions below one so cloudinary does not read them as pixels`() {
        val imageUrl = ImageUrl("https://res.cloudinary.com/demo/image/upload/v123/page.jpg")

        val cropped = cropper.crop(imageUrl, CropRegion(x = 0.0, y = 0.0, width = 1.0, height = 1.0))

        assertEquals(
            ImageUrl(
                "https://res.cloudinary.com/demo/image/upload/" +
                    "c_crop,x_0.0000,y_0.0000,w_0.9999,h_0.9999/v123/page.jpg",
            ),
            cropped,
        )
    }

    @Test
    fun `leaves a url from another cloud untouched`() {
        val imageUrl = ImageUrl("https://res.cloudinary.com/other/image/upload/v123/page.jpg")

        val cropped = cropper.crop(imageUrl, CropRegion(x = 0.1, y = 0.1, width = 0.5, height = 0.5))

        assertEquals(imageUrl, cropped)
    }

    @Test
    fun `leaves a url from another host untouched`() {
        val imageUrl = ImageUrl("https://img.spoonacular.com/recipes/716429-556x370.jpg")

        val cropped = cropper.crop(imageUrl, CropRegion(x = 0.1, y = 0.1, width = 0.5, height = 0.5))

        assertEquals(imageUrl, cropped)
    }
}
