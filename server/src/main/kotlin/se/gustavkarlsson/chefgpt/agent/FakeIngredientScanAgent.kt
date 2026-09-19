package se.gustavkarlsson.chefgpt.agent

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

class FakeIngredientScanAgent(
    private val ingredientStore: IngredientStore,
) : IngredientScanAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<ApiAttachment>,
    ): Result<Int, String> {
        val found = listOf("tomato", "basil")
        ingredientStore.createIngredients(userId, found)
        return Ok(found.size)
    }
}
