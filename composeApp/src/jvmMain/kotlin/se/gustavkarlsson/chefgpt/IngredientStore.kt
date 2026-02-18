package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val LINE_SEPARATOR = "\n"

@LLMDescription("Handles ingredients the user has at home.")
class IngredientStore(private val file: Path) : ToolSet {
    private val ingredients: MutableSet<String> = if (file.exists()) {
        file.readText()
            .split(LINE_SEPARATOR)
            .filterNot { it.isBlank() }
            .toMutableSet()
    } else {
        mutableSetOf()
    }

    @Tool
    fun getIngredients(): List<String> = ingredients.toList()

    @Tool
    fun addIngredients(ingredients: List<String>) {
        // Sanitize ingredients on storage
        this.ingredients += ingredients.map { it.trim().lowercase() }
        save()
    }

    @Tool
    fun removeIngredients(ingredients: List<String>) {
        this.ingredients -= ingredients
        save()
    }

    @Tool
    fun clearIngredients() {
        this.ingredients.clear()
        save()
    }

    private fun save() {
        val text = ingredients
            .filterNot { it.isBlank() }
            .joinToString(LINE_SEPARATOR)
        file.writeText(text)
    }
}
