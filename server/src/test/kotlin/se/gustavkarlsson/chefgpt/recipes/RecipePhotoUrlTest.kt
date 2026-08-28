package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ImageUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecipePhotoUrlTest {
    @Test
    fun `accepts an uploaded photo`() {
        val url = "https://res.cloudinary.com/demo/image/upload/v123/page.jpg"

        assertEquals(ImageUrl(url), recipePhotoUrlOrNull(url))
    }

    @Test
    fun `accepts a spoonacular photo`() {
        val url = "https://img.spoonacular.com/recipes/716429-556x370.jpg"

        assertEquals(ImageUrl(url), recipePhotoUrlOrNull(url))
    }

    @Test
    fun `rejects another host`() {
        assertNull(recipePhotoUrlOrNull("https://example.com/page.jpg"))
    }

    @Test
    fun `rejects a plaintext url`() {
        assertNull(recipePhotoUrlOrNull("http://res.cloudinary.com/demo/image/upload/v123/page.jpg"))
    }

    @Test
    fun `rejects an empty url`() {
        assertNull(recipePhotoUrlOrNull(""))
    }
}
