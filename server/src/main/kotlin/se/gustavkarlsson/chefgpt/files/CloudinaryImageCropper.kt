package se.gustavkarlsson.chefgpt.files

import se.gustavkarlsson.chefgpt.api.ImageUrl
import kotlin.math.roundToInt

/**
 * Crops by inserting a `c_crop` transformation into the delivery url, so no image is ever
 * downloaded or re-uploaded — Cloudinary renders the crop on the fly.
 */
class CloudinaryImageCropper(
    private val cloud: String,
) : ImageCropper {
    private val prefix = "https://res.cloudinary.com/$cloud/"

    override fun crop(
        imageUrl: ImageUrl,
        region: CropRegion,
    ): ImageUrl {
        val url = imageUrl.value
        if (!url.startsWith(prefix) || !url.contains(UPLOAD_SEGMENT)) return imageUrl
        val transformation =
            "c_crop" +
                ",x_${formatFraction(region.x)}" +
                ",y_${formatFraction(region.y)}" +
                ",w_${formatFraction(region.width)}" +
                ",h_${formatFraction(region.height)}"
        return ImageUrl(url.replaceFirst(UPLOAD_SEGMENT, "$UPLOAD_SEGMENT$transformation/"))
    }
}

private const val UPLOAD_SEGMENT = "/upload/"
private const val PRECISION = 10_000

// Cloudinary reads a value below 1 as a fraction of the original and 1 or above as a pixel count,
// so every fraction has to stay strictly below 1 and out of scientific notation.
private fun formatFraction(value: Double): String {
    val scaled = (value * PRECISION).roundToInt().coerceIn(0, PRECISION - 1)
    return "0." + scaled.toString().padStart(4, '0')
}
