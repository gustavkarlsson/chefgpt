package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.files.AttachmentTextLoader
import se.gustavkarlsson.chefgpt.files.CloudinaryFileUploader
import se.gustavkarlsson.chefgpt.files.CloudinaryImageCropper
import se.gustavkarlsson.chefgpt.files.FakeAttachmentTextLoader
import se.gustavkarlsson.chefgpt.files.FakeFileUploader
import se.gustavkarlsson.chefgpt.files.FakeImageCropper
import se.gustavkarlsson.chefgpt.files.FileUploader
import se.gustavkarlsson.chefgpt.files.HttpAttachmentTextLoader
import se.gustavkarlsson.chefgpt.files.ImageCropper

fun Application.createFilesModule() =
    module {
        val config = environment.config
        val type = config.property("bindings.files").getString()
        val cloud =
            when (type) {
                "cloudinary" -> config.property("cloudinary.cloud").getString()
                "fake" -> null
                else -> error("Unknown files type: '$type'. Expected 'cloudinary' or 'fake'.")
            }

        single {
            if (cloud != null) {
                val cloudinaryConfig = config.config("cloudinary")
                CloudinaryFileUploader(
                    apiKey = cloudinaryConfig.property("apiKey").getString(),
                    apiSecret = cloudinaryConfig.property("apiSecret").getString(),
                    cloud = cloud,
                )
            } else {
                FakeFileUploader()
            }
        } bind FileUploader::class

        single {
            if (cloud != null) CloudinaryImageCropper(cloud) else FakeImageCropper()
        } bind ImageCropper::class

        single {
            if (cloud != null) HttpAttachmentTextLoader() else FakeAttachmentTextLoader()
        } bind AttachmentTextLoader::class
    }
