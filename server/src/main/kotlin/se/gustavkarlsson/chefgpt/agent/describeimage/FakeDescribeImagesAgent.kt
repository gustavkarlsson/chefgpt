package se.gustavkarlsson.chefgpt.agent.describeimage

import se.gustavkarlsson.chefgpt.files.UploadedFile

class FakeDescribeImagesAgent : DescribeImagesAgent {
    override suspend fun scan(files: List<UploadedFile>): List<String> =
        listOf("A fake description of an image", "Another description")
}
