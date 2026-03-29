package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import se.gustavkarlsson.chefgpt.images.CloudinaryImageUploader
import se.gustavkarlsson.chefgpt.images.ImageUploader

fun createImageUploader(config: ApplicationConfig): ImageUploader =
    CloudinaryImageUploader(
        apiKey = config.property("apiKey").getString(),
        apiSecret = config.property("apiSecret").getString(),
        cloud = config.property("cloud").getString(),
    )
