package se.gustavkarlsson.chefgpt.agent

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

class FakeIngredientScanAgent(
    private val ingredientStore: IngredientStore,
) : IngredientScanAgent {
    override suspend fun RoutingContext.scan(
        userId: UserId,
        imageUrl: ImageUrl,
    ): Result<Int, String> {
        val found = listOf("tomato", "basil")
        ingredientStore.createIngredients(userId, found)
        return Ok(found.size)
    }
}
