package se.gustavkarlsson.chefgpt.agent.scaningredients

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentSource
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.toImageAttachmentOrNull

private val SYSTEM_PROMPT =
    """
    You are an ingredient scanner. Your only job is to look at the images and
    identify the edible food ingredients that are visible in them.
    Ignore non-food objects, such as packaging, backgrounds, utensils and people.

    Ingredient naming:
    Copy the spelling of the existing ingredients listed below (including
    capitalization) to avoid duplicates. For new ingredients, use simple,
    plain-text lowercase names (e.g. "tomatoes", "eggs", "milk", "black pepper").

    Respond with the identified ingredients, one per line, and nothing else —
    no headings, no explanations, no blank lines.
    """.trimIndent()

private val LIST_ITEM_PREFIX = Regex("""^\s*(?:[-*•+]|\d+[.)])\s+""")

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
        val scanResult = scanIngredients(imageAttachments, existingIngredients)
        val scannedIngredients = parseIngredients(scanResult)

        return matchExistingSpelling(scannedIngredients, existingIngredients).distinct()
    }
}

private fun parseIngredients(scanResult: String): List<String> =
    scanResult
        .lines()
        .map { line -> line.replace(LIST_ITEM_PREFIX, "").trim() }
        .filter { it.isNotEmpty() }

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
    ): String {
        val agent =
            AIAgent(
                promptExecutor = promptExecutor,
                agentConfig =
                    AIAgentConfig(
                        prompt = buildPrompt(images, existingIngredients),
                        model = model,
                        maxAgentIterations = 1,
                    ),
                strategy = scanIngredientsStrategy(),
            )
        return agent.run("Scan these images for ingredients and list the ones you find.")
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
    functionalStrategy<String, String>("scan-ingredients") { input ->
        getTextParts(requestLLM(input)).joinToString("\n") { it.text }
    }
