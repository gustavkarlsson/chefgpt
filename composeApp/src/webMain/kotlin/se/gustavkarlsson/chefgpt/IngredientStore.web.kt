package se.gustavkarlsson.chefgpt

private const val STORAGE_KEY = "ingredient-store"
private const val LINE_SEPARATOR = "\n"

external interface Storage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
}

external val localStorage: Storage

class WebIngredientStore : IngredientStore {
    private val ingredients: MutableSet<String> = localStorage.getItem(STORAGE_KEY)
        ?.split(LINE_SEPARATOR)
        ?.filterNot { it.isBlank() }
        ?.toMutableSet()
        ?: mutableSetOf()

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
        val text = ingredients
            .filterNot { it.isBlank() }
            .joinToString(LINE_SEPARATOR)
        localStorage.setItem(STORAGE_KEY, text)
    }
}

actual fun createIngredientStore(): IngredientStore = WebIngredientStore()