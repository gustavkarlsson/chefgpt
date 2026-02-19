package se.gustavkarlsson.chefgpt

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val LINE_SEPARATOR = "\n"

class JvmIngredientStore(
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

    override fun getIngredients(): List<String> = ingredients.toList()

    override fun addIngredients(ingredients: List<String>) {
        // Sanitize ingredients on storage
        this.ingredients += ingredients.map { it.trim().lowercase() }
        save()
    }

    override fun removeIngredients(ingredients: List<String>) {
        this.ingredients -= ingredients
        save()
    }

    override fun clearIngredients() {
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

actual fun createIngredientStore(): IngredientStore = JvmIngredientStore(Path.of("ingredient-store.txt"))
