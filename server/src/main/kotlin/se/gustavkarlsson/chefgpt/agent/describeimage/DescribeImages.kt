package se.gustavkarlsson.chefgpt.agent.describeimage

import ai.koog.prompt.message.AttachmentSource

fun interface DescribeImages {
    suspend operator fun invoke(images: List<AttachmentSource.Image>): Map<Int, String>
}
