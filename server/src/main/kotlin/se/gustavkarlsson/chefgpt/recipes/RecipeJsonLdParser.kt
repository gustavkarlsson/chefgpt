package se.gustavkarlsson.chefgpt.recipes

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import se.gustavkarlsson.chefgpt.api.common.ImageUrl
import kotlin.time.Duration

/**
 * Extracts a recipe from a page's JSON-LD (schema.org Recipe), which most recipe sites embed.
 * The ingredients' amounts and units stay in the name, since free text like "3 cups flour"
 * cannot be split reliably — the unit conversion agent keeps such amounts as written.
 */
class RecipeJsonLdParser(
    private val json: Json,
) {
    fun parseRecipe(html: String): NewRecipe? =
        jsonLdScripts(html)
            .mapNotNull { script ->
                runCatching { json.parseToJsonElement(script) }.getOrNull()?.findRecipeObject()?.toNewRecipe()
            }.firstOrNull()
}

private val JSON_LD_SCRIPT =
    Regex(
        """<script[^>]*type\s*=\s*["']application/ld\+json["'][^>]*>(.*?)</script>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

private val NUMBERS = Regex("\\d+")

private fun jsonLdScripts(html: String): List<String> =
    JSON_LD_SCRIPT.findAll(html).mapNotNull { match -> match.groupValues[1] }.toList()

private fun JsonElement.findRecipeObject(): JsonObject? {
    val obj = this as? JsonObject ?: return null
    if (obj.isRecipeObject()) return obj
    return obj["@graph"]?.jsonArray?.mapNotNull { element -> element.findRecipeObject() }?.firstOrNull()
}

private fun JsonObject.isRecipeObject(): Boolean {
    val types =
        when (val type = this["@type"] ?: return false) {
            is JsonArray -> type.mapNotNull { it.jsonPrimitive.contentOrNull }
            is JsonPrimitive -> listOfNotNull(type.contentOrNull)
            else -> emptyList()
        }
    return "Recipe" in types
}

private fun JsonObject.toNewRecipe(): NewRecipe? {
    val title = get("name")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
    return NewRecipe(
        title = title,
        steps = get("recipeInstructions")?.toSteps().orEmpty(),
        imageUrl = get("image")?.toImageUrlOrNull()?.let(::ImageUrl),
        description = get("description")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
        preparationDuration = iso8601DurationOrNull("prepTime"),
        cookingDuration = iso8601DurationOrNull("cookTime"),
        duration = iso8601DurationOrNull("totalTime"),
        servings = servingsOrNull(),
        ingredients = get("recipeIngredient")?.toIngredients().orEmpty(),
        nutrients = emptyList(),
        spoonacularId = null,
    )
}

private fun JsonElement.toSteps(): List<String> =
    when (this) {
        is JsonPrimitive -> {
            listOfNotNull(contentOrNull)
        }

        is JsonArray -> {
            flatMap { element -> element.toSteps() }
        }

        is JsonObject -> {
            when {
                containsKey("text") -> listOfNotNull(get("text")?.toStepText())
                containsKey("itemListElement") -> get("itemListElement")?.toSteps().orEmpty()
                else -> emptyList()
            }
        }

        else -> {
            emptyList()
        }
    }.filter { it.isNotBlank() }

/** A step's text can itself be a list on some sites. */
private fun JsonElement.toStepText(): String? =
    when (this) {
        is JsonPrimitive -> contentOrNull
        is JsonArray -> mapNotNull { element -> element.toStepText() }.firstOrNull()
        else -> null
    }

private fun JsonElement.toIngredients(): List<RecipeIngredient> =
    toIngredientTexts()
        .filter { it.isNotBlank() }
        .map { ingredient -> RecipeIngredient(name = ingredient, value = "", unit = null) }

private fun JsonElement.toIngredientTexts(): List<String> =
    when (this) {
        is JsonPrimitive -> listOfNotNull(contentOrNull)
        is JsonArray -> flatMap { element -> element.toIngredientTexts() }
        else -> emptyList()
    }

private fun JsonElement.toImageUrlOrNull(): String? =
    when (this) {
        is JsonPrimitive -> contentOrNull?.takeIf { it.isNotBlank() }
        is JsonArray -> mapNotNull { element -> element.toImageUrlOrNull() }.firstOrNull()
        is JsonObject -> get("url")?.toImageUrlOrNull()
        else -> null
    }

private fun JsonObject.iso8601DurationOrNull(key: String): Duration? {
    val text = get(key)?.jsonPrimitive?.contentOrNull ?: return null
    return runCatching { Duration.parseIsoString(text) }.getOrNull()?.takeIf { it.isPositive() }
}

private fun JsonObject.servingsOrNull(): IntRange? {
    val yieldText =
        when (val yield = get("recipeYield") ?: return null) {
            is JsonPrimitive -> yield.contentOrNull
            is JsonArray -> yield.mapNotNull { element -> element.jsonPrimitive.contentOrNull }.firstOrNull()
            else -> null
        } ?: return null
    val numbers = NUMBERS.findAll(yieldText).mapNotNull { match -> match.value.toIntOrNull() }.toList()
    if (numbers.isEmpty()) return null
    return numbers.min()..numbers.max()
}
