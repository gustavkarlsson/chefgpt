package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.images.CloudinaryImageUploader
import se.gustavkarlsson.chefgpt.images.FakeImageUploader
import se.gustavkarlsson.chefgpt.images.ImageUploader

fun createImageUploaderModule(config: ApplicationConfig) =
    module {
        single {
            when (val type = config.property("chefgpt.imageUploader").getString()) {
                "cloudinary" -> {
                    val cloudinaryConfig = config.config("cloudinary")
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
        } bind ImageUploader::class
    }
