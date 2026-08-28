package se.gustavkarlsson.chefgpt.recipes

import io.ktor.http.Url
import se.gustavkarlsson.chefgpt.api.ImageUrl

// The hosts we upload to and look recipes up from. Photo urls reach us from the agent, so anything
// else is dropped rather than stored and later loaded by every client showing the recipe.
private val allowedHosts = setOf("res.cloudinary.com", "img.spoonacular.com")

fun recipePhotoUrlOrNull(url: String): ImageUrl? {
    val parsed = runCatching { Url(url) }.getOrNull() ?: return null
    if (parsed.protocol.name != "https" || parsed.host !in allowedHosts) return null
    return ImageUrl(url)
}
