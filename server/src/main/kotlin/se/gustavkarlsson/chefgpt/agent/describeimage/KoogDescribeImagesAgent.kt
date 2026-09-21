package se.gustavkarlsson.chefgpt.agent.describeimage

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentSource
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.toImageAttachmentOrNull

private val logger = LoggerFactory.getLogger("KoogDescribeImagesAgent")

private val SYSTEM_PROMPT =
    """
    You describe images. Describe what the image or images show in plain text,
    factually and in enough detail to be useful. Do not invent anything that is
    not visible in the images.

    Describe every image with exactly one line, in the same order as the images.
    Start each line with the image's 0-based position in brackets:

    [0] A strawberry milkshake in a tall glass

    Do not write anything else — no headings, summaries, or blank lines. If an
    image cannot be described, write the reason instead of a description, such as
    "[3] Failed to scan image". Do not skip any files.
    """.trimIndent()

class KoogDescribeImagesAgent(
    private val describeImages: DescribeImages,
) : DescribeImagesAgent {
    constructor(promptExecutor: PromptExecutor, model: LLModel) : this(AgenticDescribeImages(promptExecutor, model))

    override suspend fun scan(files: List<UploadedFile>): List<String> {
        val imagesByFileIndex = getImagesByFileIndex(files)
        val images = imagesByFileIndex.mapNotNull { it?.second }
        if (images.isEmpty()) {
            return files.map { "Not an image" }
        }
        val descriptionsByImageIndex = describeImages.invoke(images)
        return getDescriptions(imagesByFileIndex, descriptionsByImageIndex)
    }

    private fun getImagesByFileIndex(files: List<UploadedFile>): List<Pair<Int, AttachmentSource.Image>?> {
        var skipped = 0
        return files.mapIndexed { fileIndex, file ->
            val image = file.toImageAttachmentOrNull()
            if (image != null) {
                val imageIndex = fileIndex - skipped
                imageIndex to image
            } else {
                skipped++
                null
            }
        }
    }

    private fun getDescriptions(
        imagesByFileIndex: List<Pair<Int, AttachmentSource.Image>?>,
        descriptionsByImageIndex: Map<Int, String>,
    ): List<String> =
        imagesByFileIndex.map { imageByFileIndex ->
            if (imageByFileIndex == null) {
                "Not an image"
            } else {
                val imageIndex = imageByFileIndex.first
                val description = descriptionsByImageIndex[imageIndex]
                description ?: "Skipped by agent"
            }
        }
}

private class AgenticDescribeImages(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
) : DescribeImages {
    override suspend fun invoke(images: List<AttachmentSource.Image>): Map<Int, String> {
        val agent =
            AIAgent(
                promptExecutor = promptExecutor,
                agentConfig =
                    AIAgentConfig(
                        prompt = buildPrompt(images),
                        model = model,
                        maxAgentIterations = 1,
                    ),
                strategy = describeImageStrategy(),
            )
        return agent.run("Describe these images.")
    }
}

private fun buildPrompt(imageAttachments: List<AttachmentSource.Image>) =
    prompt("describe-images") {
        system(SYSTEM_PROMPT)
        user {
            for (attachment in imageAttachments) {
                image(attachment)
            }
        }
    }

private val DESCRIPTION_LINE = Regex("""\[(\d+)]\s*(.*)""")

private fun describeImageStrategy() =
    functionalStrategy<String, Map<Int, String>>("describe-images") { input ->
        getTextParts(requestLLM(input))
            .flatMap { it.text.lines() }
            .mapNotNull { line -> DESCRIPTION_LINE.matchEntire(line) }
            .associate { match ->
                val index = match.groupValues[1].toInt()
                val description = match.groupValues[2].trim()
                index to description
            }
    }
