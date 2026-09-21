package se.gustavkarlsson.chefgpt.agent.describeimages

import se.gustavkarlsson.chefgpt.files.UploadedFile

class FakeDescribeImagesAgent : DescribeImagesAgent {
    override suspend fun run(files: List<UploadedFile>): List<String> =
        listOf("A fake description of an image", "Another description")
}
