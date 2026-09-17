package se.gustavkarlsson.chefgpt.agent

import com.github.michaelbull.result.Result
import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.auth.UserId

interface RecipeScanAgent {
    suspend fun RoutingContext.scan(
        userId: UserId,
        images: List<ApiAttachment>,
    ): Result<List<ApiRecipeSummary>, String>
}
