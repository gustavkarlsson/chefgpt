package se.gustavkarlsson.chefgpt.agent.describeimages

import ai.koog.prompt.message.AttachmentSource

fun interface DescribeImages {
    suspend operator fun invoke(images: List<AttachmentSource.Image>): String
}
