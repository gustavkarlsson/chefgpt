package se.gustavkarlsson.chefgpt.agent

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile

class FakeDescribeImageAgent : DescribeImageAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String>? = listOf("A fake description of an image", "Another description")
}
