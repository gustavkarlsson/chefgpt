package se.gustavkarlsson.chefgpt.agent.scaningredients

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentSource
import org.jetbrains.annotations.VisibleForTesting
import se.gustavkarlsson.chefgpt.agent.chat.toImageAttachmentOrNull
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

private val SYSTEM_PROMPT =
    """
    You are an ingredient scanner. Your only job is to look at the images and
    identify the edible food ingredients that are visible in them.
    Ignore non-food objects, such as packaging, backgrounds, utensils and people.

    Ingredient naming:
    Copy the spelling of the existing ingredients listed below (including
    capitalization) to avoid duplicates. For new ingredients, use simple,
    plain-text lowercase names (e.g. "tomatoes", "eggs", "milk", "black pepper").

    Report the identified ingredients by calling the reportIngredients tool,
    with an empty list when you find none. Never write the ingredients as
    plain text — always use the tools.
    """.trimIndent()

class KoogScanIngredientsAgent(
    private val scanIngredients: ScanIngredients,
    private val ingredientStore: IngredientStore,
) : ScanIngredientsAgent {
    constructor(
        promptExecutor: PromptExecutor,
        model: LLModel,
        ingredientStore: IngredientStore,
    ) : this(AgenticScanIngredients(promptExecutor, model), ingredientStore)

    override suspend fun scan(
        userId: UserId,
        files: List<UploadedFile>,
    ): List<String> {
        val imageAttachments = files.mapNotNull { it.toImageAttachmentOrNull() }
        if (imageAttachments.isEmpty()) {
            return emptyList()
        }
        val existingIngredients = ingredientStore.getIngredients(userId).map { it.name }
        val scannedIngredients =
            scanIngredients(imageAttachments, existingIngredients).filter { it.isNotBlank() }

        return matchExistingSpelling(scannedIngredients, existingIngredients).distinct()
    }
}

private fun matchExistingSpelling(
    scanned: List<String>,
    existingIngredients: List<String>,
): List<String> {
    val existingByLowercase = existingIngredients.associateBy { it.lowercase() }
    return scanned.map { ingredient -> existingByLowercase[ingredient.lowercase()] ?: ingredient }
}

private class AgenticScanIngredients(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
) : ScanIngredients {
    override suspend fun invoke(
        images: List<AttachmentSource.Image>,
        existingIngredients: List<String>,
    ): List<String> {
        val reportTool = ReportIngredientsTool()
        val agent =
            AIAgent(
                promptExecutor = promptExecutor,
                agentConfig =
                    AIAgentConfig(
                        prompt = buildPrompt(images, existingIngredients),
                        model = model,
                        maxAgentIterations = 5,
                    ),
                strategy = scanIngredientsStrategy(),
                toolRegistry = ToolRegistry { tools(reportTool) },
            )
        agent.run("Scan these images for ingredients and report the ones you find.")
        return reportTool.ingredients
    }
}

private fun buildPrompt(
    images: List<AttachmentSource.Image>,
    existingIngredients: List<String>,
) = prompt("scan-ingredients") {
    system(SYSTEM_PROMPT)
    system(existingIngredientsText(existingIngredients))
    user {
        for (image in images) {
            image(image)
        }
    }
}

private fun existingIngredientsText(existingIngredients: List<String>): String =
    if (existingIngredients.isEmpty()) {
        "There are no existing ingredients yet."
    } else {
        "Existing ingredients:\n" + existingIngredients.joinToString("\n")
    }

private fun scanIngredientsStrategy() =
    functionalStrategy<String, Unit>("scan-ingredients") { input ->
        var message = requestLLM(input)
        repeat(config.maxAgentIterations) {
            val toolCalls = getToolCalls(message)
            if (toolCalls.isEmpty()) {
                return@repeat
            }
            val toolResults = executeTools(toolCalls)
            message = sendToolResults(toolResults)
        }
    }

/**
 * Receives the ingredients the scan agent's LLM identified in the photos.
 */
@VisibleForTesting
class ReportIngredientsTool : ToolSet {
    var ingredients: List<String> = emptyList()
        private set

    @Tool
    @LLMDescription(
        "Report the ingredients you identified in the photos. Call this once, " +
            "with an empty list when you find none.",
    )
    suspend fun reportIngredients(
        @LLMDescription("The names of the identified ingredients.")
        ingredients: List<String>,
    ): String {
        this.ingredients = ingredients
        return "Saved the reported ingredients."
    }
}
