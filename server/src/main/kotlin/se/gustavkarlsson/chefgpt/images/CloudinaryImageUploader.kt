package se.gustavkarlsson.chefgpt.images

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.util.url
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import se.gustavkarlsson.chefgpt.api.ImageUrl

class CloudinaryImageUploader(
    private val apiKey: String,
    private val apiSecret: String,
    private val cloud: String,
) : ImageUploader,
    AutoCloseable {
    private val client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
            }
            install(ContentNegotiation) {
                json(Json)
            }
            // TODO Install logging
        }

    override suspend fun uploadImage(
        readChannel: ByteReadChannel,
        contentType: ContentType?,
    ): ImageUrl {
        val jsonObject =
            client
                .submitFormWithBinaryData(
                    url =
                        url {
                            protocol = URLProtocol.HTTPS
                            host = "api.cloudinary.com"
                            pathSegments = listOf("v1_1", cloud, "image", "upload")
                        },
                    formData =
                        formData {
                            append(
                                key = "file",
                                value = ChannelProvider { readChannel },
                                headers =
                                    headers {
                                        val finalFileName =
                                            buildString {
                                                append("image")
                                                if (contentType?.contentType == "image") {
                                                    append(".${contentType.contentSubtype}")
                                                }
                                            }
                                        append(HttpHeaders.ContentDisposition, "filename=$finalFileName")
                                        contentType?.let { append(HttpHeaders.ContentType, it.toString()) }
                                    },
                            )
                        },
                ) {
                    basicAuth(apiKey, apiSecret)
                }.body<JsonObject>()
        val url =
            jsonObject
                .getValue("secure_url")
                .jsonPrimitive.content
        return ImageUrl(url)
    }

    override fun close() {
        client.close()
    }
}

fun createCloudinaryImageUploader(config: ApplicationConfig): ImageUploader =
    CloudinaryImageUploader(
        apiKey = config.property("apiKey").getString(),
        apiSecret = config.property("apiSecret").getString(),
        cloud = config.property("cloud").getString(),
    )
