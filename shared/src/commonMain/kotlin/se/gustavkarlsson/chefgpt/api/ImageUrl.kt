package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.StringValueSerializer
import kotlin.jvm.JvmInline

@Serializable(with = ImageUrlSerializer::class)
@JvmInline
value class ImageUrl(
    val value: String,
) {
    override fun toString(): String = value
}

object ImageUrlSerializer : StringValueSerializer<ImageUrl>("image-url", ::ImageUrl, ImageUrl::value)
