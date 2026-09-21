package se.gustavkarlsson.chefgpt.agent.scaningredients

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

class FakeScanIngredientsAgent(
    private val ingredientStore: IngredientStore,
) : ScanIngredientsAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String> {
        val found = listOf("tomato", "basil")
        ingredientStore.createIngredients(userId, found)
        return found
    }
}
