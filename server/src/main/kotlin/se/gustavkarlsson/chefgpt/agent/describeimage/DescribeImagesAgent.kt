package se.gustavkarlsson.chefgpt.agent.describeimage

import se.gustavkarlsson.chefgpt.files.UploadedFile

interface DescribeImagesAgent {
    suspend fun scan(files: List<UploadedFile>): List<String>
}
