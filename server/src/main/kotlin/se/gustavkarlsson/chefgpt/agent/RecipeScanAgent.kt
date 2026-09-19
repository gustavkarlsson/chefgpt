package se.gustavkarlsson.chefgpt.agent

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.auth.UserId

interface RecipeScanAgent {
    suspend fun scan(
        userId: UserId,
        images: List<ApiAttachment>,
    ): Result<List<ApiRecipeSummary>, String>
}
