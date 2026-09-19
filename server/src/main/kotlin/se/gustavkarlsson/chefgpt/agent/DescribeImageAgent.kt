package se.gustavkarlsson.chefgpt.agent

import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.auth.UserId

interface DescribeImageAgent {
    suspend fun scan(
        userId: UserId,
        images: List<ApiAttachment>,
    ): Result<String, String>
}
