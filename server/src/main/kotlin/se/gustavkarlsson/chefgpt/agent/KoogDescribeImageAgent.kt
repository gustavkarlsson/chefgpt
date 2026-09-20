package se.gustavkarlsson.chefgpt.agent
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import org.slf4j.LoggerFactory
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.toImageAttachmentOrNull

private val logger = LoggerFactory.getLogger("KoogDescribeImageAgent")

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

class KoogDescribeImageAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
) : DescribeImageAgent {
    override suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String> {
        val agent = buildAgent(buildPrompt(images))
        return agent.run("Describe these images.")
    }

    private fun buildAgent(prompt: Prompt) =
        AIAgent(
            promptExecutor = promptExecutor,
            agentConfig =
                AIAgentConfig(
                    prompt = prompt,
                    model = model,
                    maxAgentIterations = 1,
                ),
            strategy = describeImageStrategy(),
            toolRegistry = ToolRegistry {},
        )
}

private fun buildPrompt(images: List<UploadedFile>) =
    prompt("describe-images") {
        system(SYSTEM_PROMPT)
        user {
            for (image in images) {
                image.toImageAttachmentOrNull()?.let {
                    image(it)
                }
            }
        }
    }

private val DESCRIPTION_LINE = Regex("""\[\d+]\s*(.*)""")

private fun describeImageStrategy() =
    functionalStrategy<String, List<String>>("describe-images") { input ->
        getTextParts(requestLLM(input))
            .flatMap { it.text.lines() }
            .mapNotNull { line -> DESCRIPTION_LINE.matchEntire(line) }
            .map { match -> match.groupValues[1].trim() }
            .toList()
    }
