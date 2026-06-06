package se.gustavkarlsson.chefgpt.agent

import com.github.michaelbull.result.Result
import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.auth.UserId

interface IngredientScanAgent {
    suspend fun RoutingContext.scan(
        userId: UserId,
        imageUrl: ImageUrl,
    ): Result<Int, String>
}
