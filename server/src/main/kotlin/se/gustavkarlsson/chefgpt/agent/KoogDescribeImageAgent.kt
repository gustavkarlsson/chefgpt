package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.format

private val SYSTEM_PROMPT =
    """
    You describe images. Describe what the image or images show in plain text,
    factually and in enough detail to be useful. Do not invent anything that is
    not visible in the images.
    """.trimIndent()

class KoogDescribeImageAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
) : DescribeImageAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<ApiAttachment>,
    ): Result<String, String> {
        val agent =
            AIAgent(
                promptExecutor = promptExecutor,
                agentConfig =
                    AIAgentConfig(
                        prompt =
                            prompt("describe-images") {
                                system(SYSTEM_PROMPT)
                                user {
                                    images.forEach { image ->
                                        image(
                                            AttachmentSource.Image(
                                                AttachmentContent.URL(image.url),
                                                image.format,
                                                image.mimeType,
                                                image.fileName,
                                            ),
                                        )
                                    }
                                }
                            },
                        model = model,
                        maxAgentIterations = 1,
                    ),
                toolRegistry = ToolRegistry {},
            )
        return Ok(agent.run("Describe these images."))
    }
}
