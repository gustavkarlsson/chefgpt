package se.gustavkarlsson.chefgpt.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val LINE_SEPARATOR = "\n"

class IngredientStore(
    private val file: Path,
) : ToolSet {
    private val ingredients: MutableSet<String> =
        if (file.exists()) {
            file
                .readText()
                .split(LINE_SEPARATOR)
                .filterNot { it.isBlank() }
                .toMutableSet()
        } else {
            mutableSetOf()
        }

    @Tool
    @LLMDescription("Get all user's stored ingredients")
    fun getIngredients(): List<String> = ingredients.toList()

    @Tool
    @LLMDescription("Add the given ingredients to the user's store")
    fun addIngredients(ingredients: List<String>) {
        // Sanitize ingredients on storage
        this.ingredients += ingredients.map { it.trim().lowercase() }
        save()
    }

    @Tool
    @LLMDescription("Remove the given ingredients from the user's store")
    fun removeIngredients(ingredients: List<String>) {
        this.ingredients -= ingredients
        save()
    }

    @Tool
    @LLMDescription("Clear all ingredients from the user's store")
    fun clearIngredients() {
        this.ingredients.clear()
        save()
    }

    private fun save() {
        val text =
            ingredients
                .filterNot { it.isBlank() }
                .joinToString(LINE_SEPARATOR)
        file.writeText(text)
    }
}
