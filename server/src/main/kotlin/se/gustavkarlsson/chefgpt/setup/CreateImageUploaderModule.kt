package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.images.CloudinaryImageUploader
import se.gustavkarlsson.chefgpt.images.FakeImageUploader
import se.gustavkarlsson.chefgpt.images.ImageUploader

fun Application.createImageUploaderModule() =
    module {
        val config = environment.config
        single {
            when (val type = config.property("bindings.imageUploader").getString()) {
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
