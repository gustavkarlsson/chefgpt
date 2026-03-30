package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import se.gustavkarlsson.chefgpt.images.CloudinaryImageUploader
import se.gustavkarlsson.chefgpt.images.FakeImageUploader
import se.gustavkarlsson.chefgpt.images.ImageUploader

fun createImageUploader(
    type: String,
    config: ApplicationConfig,
): ImageUploader =
    when (type) {
        "cloudinary" -> {
            val cloudinaryConfig = config.config("chefgpt.cloudinary")
            CloudinaryImageUploader(
                apiKey = cloudinaryConfig.property("apiKey").getString(),
                apiSecret = cloudinaryConfig.property("apiSecret").getString(),
                cloud = cloudinaryConfig.property("cloud").getString(),
            )
        }

        "fake" -> {
            FakeImageUploader()
        }

        else -> {
            error("Unknown image uploader type: '$type'. Expected 'cloudinary' or 'fake'.")
        }
    }
