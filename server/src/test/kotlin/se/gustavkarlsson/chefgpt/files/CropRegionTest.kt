package se.gustavkarlsson.chefgpt.files

import kotlin.test.Test
import kotlin.test.assertFailsWith

class CropRegionTest {
    @Test
    fun `rejects a zero sized region`() {
        assertFailsWith<IllegalArgumentException> {
            CropRegion(x = 0.1, y = 0.1, width = 0.0, height = 0.5)
        }
    }

    @Test
    fun `rejects a negative offset`() {
        assertFailsWith<IllegalArgumentException> {
            CropRegion(x = -0.1, y = 0.1, width = 0.5, height = 0.5)
        }
    }

    @Test
    fun `rejects a region reaching past the edge`() {
        assertFailsWith<IllegalArgumentException> {
            CropRegion(x = 0.7, y = 0.1, width = 0.5, height = 0.5)
        }
    }
}
