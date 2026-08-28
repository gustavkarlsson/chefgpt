package se.gustavkarlsson.chefgpt.files

import se.gustavkarlsson.chefgpt.api.ImageUrl

class FakeImageCropper : ImageCropper {
    override fun crop(
        imageUrl: ImageUrl,
        region: CropRegion,
    ): ImageUrl = imageUrl
}
