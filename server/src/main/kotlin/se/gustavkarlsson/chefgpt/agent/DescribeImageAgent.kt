package se.gustavkarlsson.chefgpt.agent

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile

interface DescribeImageAgent {
    suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String>
}
