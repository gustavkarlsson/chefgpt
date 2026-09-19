package se.gustavkarlsson.chefgpt.agent

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.auth.UserId

class FakeDescribeImageAgent : DescribeImageAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<ApiAttachment>,
    ): Result<String, String> = Ok("A fake description of the images.")
}
