package se.gustavkarlsson.chefgpt.agent

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

class FakeIngredientScanAgent(
    private val ingredientStore: IngredientStore,
) : IngredientScanAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String>? {
        val found = listOf("tomato", "basil")
        ingredientStore.createIngredients(userId, found)
        return found
    }
}
