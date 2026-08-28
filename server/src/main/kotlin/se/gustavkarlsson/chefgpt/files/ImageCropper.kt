package se.gustavkarlsson.chefgpt.files

import se.gustavkarlsson.chefgpt.api.ImageUrl

/**
 * A rectangle expressed as fractions of the image, so a caller never needs to know its pixel size.
 */
data class CropRegion(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {
    init {
        require(width > 0.0 && height > 0.0) {
            "Crop must have a positive size"
        }
        require(x >= 0.0 && y >= 0.0 && x + width <= 1.0 && y + height <= 1.0) {
            "Crop must be inside the image"
        }
    }
}

interface ImageCropper {
    // Returns a url showing only [region] of the image, or the original url if it can't be cropped.
    fun crop(
        imageUrl: ImageUrl,
        region: CropRegion,
    ): ImageUrl
}
