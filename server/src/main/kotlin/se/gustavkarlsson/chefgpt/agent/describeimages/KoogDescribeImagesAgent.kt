package se.gustavkarlsson.chefgpt.agent.describeimages

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

private val DESCRIPTION_LINE = Regex("""\[(\d+)]\s*(.*)""")
private const val NOT_IMAGE_DESCRIPTION = "Not an image"
private const val SKIPPED_BY_AGENT_DESCRIPTION = "Skipped by agent"

class KoogDescribeImagesAgent(
    private val describeImages: DescribeImages,
) : DescribeImagesAgent {
    constructor(promptExecutor: PromptExecutor, model: LLModel) : this(AgenticDescribeImages(promptExecutor, model))

    override suspend fun run(files: List<UploadedFile>): List<String> {
        val imagesByFileIndex = getImagesByFileIndex(files)
        val images = imagesByFileIndex.mapNotNull { it?.second }
        if (images.isEmpty()) {
            return files.map { NOT_IMAGE_DESCRIPTION }
        }
        val description = describeImages(images)
        return parseDescriptions(imagesByFileIndex, description)
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

    private fun parseDescriptions(
        imagesByFileIndex: List<Pair<Int, AttachmentSource.Image>?>,
        description: String,
    ): List<String> {
        val descriptionsByImageIndex =
            description
                .lines()
                .mapNotNull { line -> DESCRIPTION_LINE.matchEntire(line) }
                .associate { match ->
                    val index = match.groupValues[1].toInt()
                    val description = match.groupValues[2].trim()
                    index to description
                }
        val expectedIndices = (0 until imagesByFileIndex.count { it != null }).toSet()
        val alignedDescriptionsByImageIndex =
            when (val describedIndices = descriptionsByImageIndex.keys) {
                expectedIndices -> {
                    descriptionsByImageIndex
                }

                expectedIndices.map { it + 1 }.toSet() -> {
                    logger.warn(
                        "Model described image indices ${describedIndices.sorted()} off by one, shifting to ${expectedIndices.sorted()}",
                    )
                    descriptionsByImageIndex.mapKeys { (index, _) -> index - 1 }
                }

                else -> {
                    logger.error(
                        "Model described image indices ${describedIndices.sorted()} but expected ${expectedIndices.sorted()}",
                    )
                    descriptionsByImageIndex
                }
            }
        return imagesByFileIndex.map { imageByFileIndex ->
            if (imageByFileIndex == null) {
                NOT_IMAGE_DESCRIPTION
            } else {
                val imageIndex = imageByFileIndex.first
                val imageDescription = alignedDescriptionsByImageIndex[imageIndex]
                imageDescription ?: SKIPPED_BY_AGENT_DESCRIPTION
            }
        }
    }
}

private class AgenticDescribeImages(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
) : DescribeImages {
    override suspend fun invoke(images: List<AttachmentSource.Image>): String {
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

private fun describeImageStrategy() =
    functionalStrategy<String, String>("describe-images") { input ->
        getTextParts(requestLLM(input)).joinToString("\n\n") { it.text }
    }
