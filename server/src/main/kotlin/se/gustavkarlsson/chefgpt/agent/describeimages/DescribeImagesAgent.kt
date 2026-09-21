package se.gustavkarlsson.chefgpt.agent.describeimages

import se.gustavkarlsson.chefgpt.files.UploadedFile

interface DescribeImagesAgent {
    suspend fun run(files: List<UploadedFile>): List<String>
}
