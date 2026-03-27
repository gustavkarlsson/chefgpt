package se.gustavkarlsson.chefgpt.tools

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val LINE_SEPARATOR = "\n"

class FileIngredientStore(
    private val file: Path,
) : IngredientStore {
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

    override suspend fun getIngredients(): List<String> = ingredients.toList()

    override suspend fun addIngredients(ingredients: List<String>): List<String> {
        val added = ingredients.map { it.trim().lowercase() }.filter { this.ingredients.add(it) }
        save()
        return added
    }

    override suspend fun removeIngredients(ingredients: List<String>): List<String> {
        val removed = ingredients.map { it.trim().lowercase() }.filter { this.ingredients.remove(it) }
        save()
        return removed
    }

    override suspend fun clearIngredients(): List<String> {
        val removed = ingredients.toList()
        this.ingredients.clear()
        save()
        return removed
    }

    private fun save() {
        val text =
            ingredients
                .filterNot { it.isBlank() }
                .joinToString(LINE_SEPARATOR)
        file.writeText(text)
    }
}
