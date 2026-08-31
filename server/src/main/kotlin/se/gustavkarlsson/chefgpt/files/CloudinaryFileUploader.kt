package se.gustavkarlsson.chefgpt.files

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
import io.ktor.server.util.url
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.chefGptJson

class CloudinaryFileUploader(
    private val apiKey: String,
    private val apiSecret: String,
    private val cloud: String,
) : FileUploader,
    AutoCloseable {
    private val client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
            }
            install(ContentNegotiation) {
                // Forgiving: Cloudinary's responses carry far more than we model.
                json(chefGptJson(strict = false))
            }
            // TODO Install logging
        }

    override suspend fun uploadFile(
        readChannel: ByteReadChannel,
        contentType: ContentType?,
        fileName: String?,
    ): ApiAttachment? =
        try {
            val mimeType = contentType?.let { "${it.contentType}/${it.contentSubtype}" } ?: "application/octet-stream"
            val jsonObject =
                client
                    .submitFormWithBinaryData(
                        url =
                            url {
                                protocol = URLProtocol.HTTPS
                                host = "api.cloudinary.com"
                                pathSegments = listOf("v1_1", cloud, resourceType(mimeType), "upload")
                            },
                        formData =
                            formData {
                                append(
                                    key = "file",
                                    value = ChannelProvider { readChannel },
                                    headers =
                                        headers {
                                            val finalFileName = fileName ?: defaultFileName(mimeType)
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
            ApiAttachment(url = url, mimeType = mimeType, fileName = fileName)
        } catch (_: Exception) {
            // TODO log error
            null
        }

    override fun close() {
        client.close()
    }
}

// Cloudinary stores PDFs as image resources, which is also what lets us crop a page of one.
private fun resourceType(mimeType: String): String =
    when (attachmentKindOrNull(mimeType)) {
        AttachmentKind.Image, AttachmentKind.Pdf -> "image"
        AttachmentKind.Text, null -> "raw"
    }

private fun defaultFileName(mimeType: String): String = "file.${mimeType.substringAfter('/').substringBefore(';')}"
