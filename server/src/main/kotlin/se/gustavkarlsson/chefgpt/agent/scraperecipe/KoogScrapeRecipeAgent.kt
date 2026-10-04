package se.gustavkarlsson.chefgpt.agent.scraperecipe

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import org.jetbrains.annotations.VisibleForTesting
import se.gustavkarlsson.chefgpt.agent.tools.models.ToolRecipeIngredient
import se.gustavkarlsson.chefgpt.agent.tools.models.toDomain
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.files.HtmlLoader
import se.gustavkarlsson.chefgpt.recipes.NewRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeIngredient
import kotlin.time.Duration.Companion.minutes

private val SYSTEM_PROMPT =
    """
    You scrape a recipe from a web page.

    Extract the recipe exactly as the page writes it: its title, its ingredients with their
    amounts and units, and its cooking steps in order. Keep the page's wording for every
    step — never skip, merge, summarize, or invent steps. Keep amounts and units exactly as
    written; a later step converts them to the user's units.

    Ignore ads, navigation, comments, and everything that is not the recipe.

    When the provided HTML is clearly incomplete, or the page points to the full recipe on
    another page, fetch the right page with the fetchPage tool. When you have the complete
    recipe, report it with the reportRecipe tool. Never write the recipe as plain text —
    always use the tools.
    """.trimIndent()

class KoogScrapeRecipeAgent(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val htmlLoader: HtmlLoader,
) : ScrapeRecipeAgent {
    override suspend fun scrape(
        url: String,
        partialRecipe: NewRecipe?,
    ): NewRecipe {
        val html = htmlLoader.loadText(url)
        val reportTool = ReportRecipeTool()
        val agent = buildAgent(url, html, partialRecipe, reportTool)
        agent.run("Scrape the recipe from the page and report it.")
        // The fallback cannot compute nutrients, so reuse the partial result's.
        return reportTool.toNewRecipe().copy(nutrients = partialRecipe?.nutrients.orEmpty())
    }

    private fun buildAgent(
        url: String,
        html: String?,
        partialRecipe: NewRecipe?,
        reportTool: ReportRecipeTool,
    ) = AIAgent(
        promptExecutor = promptExecutor,
        agentConfig =
            AIAgentConfig(
                prompt = buildPrompt(url, html, partialRecipe),
                model = model,
                maxAgentIterations = 5,
            ),
        strategy = scrapeRecipeStrategy(),
        toolRegistry =
            ToolRegistry {
                tools(reportTool)
                tools(FetchPageTool(htmlLoader))
            },
    )
}

private fun buildPrompt(
    url: String,
    html: String?,
    partialRecipe: NewRecipe?,
): Prompt =
    prompt("scrape-recipe") {
        system(SYSTEM_PROMPT)
        user {
            text(
                buildString {
                    appendLine("URL: $url")
                    partialRecipe?.let { partial ->
                        appendLine()
                        appendLine(
                            "A different scraper partially succeeded on this page and found this much, before failing:",
                        )
                        appendLine(partialText(partial))
                        appendLine()
                        appendLine(
                            "Reuse its values where the page agrees — especially its nutrients, which you cannot compute yourself.",
                        )
                    }
                    appendLine()
                    if (html == null) {
                        appendLine("The page's HTML could not be fetched. Fetch it yourself with the fetchPage tool.")
                    } else {
                        appendLine("Page HTML (may be truncated):")
                        appendLine(html)
                    }
                },
            )
        }
    }

private fun partialText(recipe: NewRecipe): String =
    buildString {
        appendLine("Title: ${recipe.title}")
        appendLine("Description: ${recipe.description ?: "(none)"}")
        recipe.preparationDuration?.let { appendLine("Preparation: $it") }
        recipe.cookingDuration?.let { appendLine("Cooking: $it") }
        recipe.duration?.let { appendLine("Total: $it") }
        recipe.servings?.let { appendLine("Servings: $it") }
        appendLine("Ingredients:")
        appendLine(ingredientsText(recipe.ingredients))
        appendLine("Nutrients:")
        appendLine(
            recipe.nutrients
                .joinToString("\n") { nutrient ->
                    listOf(nutrient.name, nutrient.value, nutrient.unit.orEmpty())
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                }.ifEmpty { "(none)" },
        )
    }

private fun ingredientsText(ingredients: List<RecipeIngredient>): String =
    ingredients.joinToString("\n") { ingredient ->
        listOf(ingredient.name, ingredient.value, ingredient.unit.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

private fun scrapeRecipeStrategy() =
    functionalStrategy<String, Unit>("scrape-recipe") { input ->
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
 * Fetches a page's HTML for the agent, so it can reach the recipe itself when the initial fetch missed it.
 */
@VisibleForTesting
class FetchPageTool(
    private val htmlLoader: HtmlLoader,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Fetch a web page's HTML. Use this to fetch the recipe page itself, or another page it links to.",
    )
    suspend fun fetchPage(
        @LLMDescription("The full URL of the page to fetch.")
        url: String,
    ): String = htmlLoader.loadText(url) ?: "Could not fetch $url"
}

/**
 * Receives the recipe the scrape agent's LLM extracted. Optional fields use sentinel defaults,
 * which Koog cannot make truly optional, and are treated as absent.
 */
@VisibleForTesting
class ReportRecipeTool : ToolSet {
    var title: String? = null
        private set
    var ingredients: List<ToolRecipeIngredient>? = null
        private set
    var steps: List<String>? = null
        private set
    var imageUrl: String? = null
        private set
    var description: String? = null
        private set
    var preparationMinutes: Int? = null
        private set
    var cookingMinutes: Int? = null
        private set
    var totalMinutes: Int? = null
        private set
    var servings: Int? = null
        private set

    @Tool
    @LLMDescription("Report the recipe extracted from the page. Call this once with every field filled in.")
    suspend fun reportRecipe(
        @LLMDescription("The recipe's title.")
        title: String,
        @LLMDescription("The ingredients with their amounts and units, exactly as written on the page.")
        ingredients: List<ToolRecipeIngredient>,
        @LLMDescription("The cooking steps in order, exactly as written on the page.")
        steps: List<String>,
        @LLMDescription("A direct URL to a photo of the dish on the page, or an empty string when there is none.")
        imageUrl: String = "",
        @LLMDescription("The recipe's description or summary, or an empty string when the page has none.")
        description: String = "",
        @LLMDescription("Preparation time in minutes, or 0 when the page does not state it.")
        preparationMinutes: Int = 0,
        @LLMDescription("Cooking time in minutes, or 0 when the page does not state it.")
        cookingMinutes: Int = 0,
        @LLMDescription("Total time in minutes, or 0 when the page does not state it.")
        totalMinutes: Int = 0,
        @LLMDescription("The number of servings, or 0 when the page does not state it.")
        servings: Int = 0,
    ): String {
        this.title = title
        this.ingredients = ingredients
        this.steps = steps
        this.imageUrl = imageUrl
        this.description = description
        this.preparationMinutes = preparationMinutes
        this.cookingMinutes = cookingMinutes
        this.totalMinutes = totalMinutes
        this.servings = servings
        return "Saved the reported recipe."
    }

    fun toNewRecipe(): NewRecipe =
        NewRecipe(
            title = title.orEmpty(),
            steps = steps.orEmpty(),
            imageUrl = imageUrl?.takeIf { it.isNotBlank() }?.let(::ImageUrl),
            description = description?.takeIf { it.isNotBlank() },
            preparationDuration = preparationMinutes?.takeIf { it > 0 }?.minutes,
            cookingDuration = cookingMinutes?.takeIf { it > 0 }?.minutes,
            duration = totalMinutes?.takeIf { it > 0 }?.minutes,
            servings = servings?.takeIf { it > 0 }?.let { it..it },
            ingredients = ingredients.orEmpty().map { it.toDomain() },
            nutrients = emptyList(),
            spoonacularId = null,
        )
}
