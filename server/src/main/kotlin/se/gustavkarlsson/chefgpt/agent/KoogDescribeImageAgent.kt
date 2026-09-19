package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile

private val logger = LoggerFactory.getLogger("KoogDescribeImageAgent")

private val SYSTEM_PROMPT =
    """
    You describe images. Describe what the image or images show in plain text,
    factually and in enough detail to be useful. Do not invent anything that is
    not visible in the images.

    REQUIREMENTS on the response:
    - Write exactly ONE single response message and only when you are finished
    - Plain text
    - One line per file containing the description of that file, in the order the files came in.
    - No extra blank lines before, after, between, or between descriptions.
    - No skipped files
    - If a file is not an image or cannot be described, state that as the description
    YOU MUST ADHERE STRICTLY TO THESE RULES
    """.trimIndent()

class KoogDescribeImageAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
) : DescribeImageAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String>? =
        try {
            val agent =
                AIAgent(
                    promptExecutor = promptExecutor,
                    agentConfig =
                        AIAgentConfig(
                            prompt =
                                prompt("describe-images") {
                                    system(SYSTEM_PROMPT)
                                    user {
                                        for (image in images) {
                                            image.toImageAttachmentOrNull()?.let {
                                                image(it)
                                            }
                                        }
                                    }
                                },
                            model = model,
                            maxAgentIterations = 1,
                        ),
                    toolRegistry = ToolRegistry {},
                )
            val rawResponse = agent.run("Describe these images.")
            rawResponse.lines()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to describe images", e)
            null
        }
}
