package se.gustavkarlsson.chefgpt.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.gustavkarlsson.chefgpt.auth.UserId
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val FILE_EXTENSION = ".txt"
private const val LINE_SEPARATOR = "\n"

class FileIngredientStore(
    rootDir: Path,
    userId: UserId,
) : IngredientStore {
    private val file: Path = rootDir.resolve("$userId$FILE_EXTENSION")

    override suspend fun getIngredients(): List<String> = transaction { toList() }

    override suspend fun addIngredients(ingredients: List<String>): List<String> =
        transaction { ingredients.map { it.trim().lowercase() }.filter { add(it) } }

    override suspend fun removeIngredients(ingredients: List<String>): List<String> =
        transaction { ingredients.map { it.trim().lowercase() }.filter { remove(it) } }

    override suspend fun clearIngredients(): List<String> =
        transaction {
            val removed = toList()
            clear()
            removed
        }

    private suspend fun <T> transaction(block: MutableSet<String>.() -> T): T =
        withContext(Dispatchers.IO) {
            val ingredients =
                if (file.exists()) {
                    file
                        .readText()
                        .split(LINE_SEPARATOR)
                        .filterNot { it.isBlank() }
                        .toMutableSet()
                } else {
                    mutableSetOf()
                }
            val snapshot = ingredients.toSet()
            val result = ingredients.block()
            if (ingredients != snapshot) {
                file.parent.createDirectories()
                file.writeText(ingredients.filterNot { it.isBlank() }.joinToString(LINE_SEPARATOR))
            }
            result
        }
}
