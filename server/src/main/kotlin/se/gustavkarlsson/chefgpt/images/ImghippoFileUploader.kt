package se.gustavkarlsson.chefgpt.images

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import se.gustavkarlsson.chefgpt.api.ImageUrl

class ImghippoFileUploader(
    private val apiKey: String,
) : ImageUploader,
    AutoCloseable {
    private val client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
            }
            install(ContentNegotiation) {
                json(Json)
            }
        }

    override suspend fun uploadImage(
        readChannel: ByteReadChannel,
        contentType: ContentType?,
    ): ImageUrl {
        val jsonObject =
            client
                .submitFormWithBinaryData(
                    url = "https://api.imghippo.com/v1/upload",
                    formData =
                        formData {
                            append("api_key", apiKey)
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
                ).body<JsonObject>()
        val url =
            jsonObject
                .getValue("data")
                .jsonObject
                .getValue("url")
                .jsonPrimitive.content
        return ImageUrl(url)
    }

    override fun close() {
        client.close()
    }
}
