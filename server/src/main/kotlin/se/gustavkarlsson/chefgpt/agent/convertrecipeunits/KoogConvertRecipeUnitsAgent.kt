package se.gustavkarlsson.chefgpt.agent.convertrecipeunits

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
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.facts.toMeasurementPromptText
import se.gustavkarlsson.chefgpt.recipes.NewRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeUpdate
import se.gustavkarlsson.chefgpt.recipes.applyUpdate

private val SYSTEM_PROMPT =
    """
    You convert the units in a recipe to the user's preferred units.

    Apply the user's measurement preference to compressible dry goods, viscous or
    sticky liquids, and irregular solids. Easy-to-pour liquids stay volume, and
    amounts not given as weight or volume (cloves, pinches, dashes) stay as
    written. Where a preference is unknown, keep the recipe's original amount and
    unit.

    Convert temperatures too — oven temperatures, cooking and baking
    temperatures, and any other temperature mentioned in the steps or description
    — to the user's temperature unit.

    Where a value is given in several units at once (for example "200 C (392 F)"),
    keep only the user's unit and drop the rest.

    Convert only the amounts, units, and temperatures — never change names,
    meaning, order, or any other wording. Preserve formatting exactly, changing
    only the amounts, units, and temperatures.

    Report each converted part by calling its tool: reportIngredients with the
    converted ingredients, reportSteps with the converted steps, and
    reportDescription with the converted description when the recipe has one.
    Never write the converted recipe as plain text — always use the tools.
    """.trimIndent()

class KoogConvertRecipeUnitsAgent(
    private val convertRecipeUnits: ConvertRecipeUnits,
    private val factRepository: FactRepository,
) : ConvertRecipeUnitsAgent {
    constructor(
        promptExecutor: PromptExecutor,
        model: LLModel,
        factRepository: FactRepository,
    ) : this(AgenticConvertRecipeUnits(promptExecutor, model), factRepository)

    override suspend fun convert(
        userId: UserId,
        recipe: NewRecipe,
    ): NewRecipe {
        val facts = factRepository.getFacts(userId)
        val update = convertRecipeUnits(recipe, facts)
        return recipe.applyUpdate(update)
    }
}

private class AgenticConvertRecipeUnits(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
) : ConvertRecipeUnits {
    override suspend fun invoke(
        recipe: NewRecipe,
        facts: UserFacts,
    ): RecipeUpdate {
        val tool = ConvertRecipeMeasurementsTool()
        val agent =
            AIAgent(
                promptExecutor = promptExecutor,
                agentConfig =
                    AIAgentConfig(
                        prompt = buildPrompt(recipe, facts),
                        model = model,
                        maxAgentIterations = 5,
                    ),
                strategy = convertRecipeUnitsStrategy(),
                toolRegistry = ToolRegistry { tools(tool) },
            )
        agent.run("Convert the recipe's units and report the results.")
        return RecipeUpdate(
            ingredients = tool.ingredients,
            description = tool.description?.takeIf { it.isNotBlank() },
            steps = tool.steps,
        )
    }
}

private fun buildPrompt(
    recipe: NewRecipe,
    facts: UserFacts,
): Prompt =
    prompt("convert-recipe-units") {
        system(SYSTEM_PROMPT)
        system(facts.toMeasurementPromptText())
        user {
            text(
                buildString {
                    appendLine("Ingredients:")
                    appendLine(ingredientsText(recipe.ingredients))
                    recipe.description?.let {
                        appendLine()
                        appendLine("Description:")
                        appendLine(it)
                    }
                    appendLine()
                    appendLine("Steps:")
                    appendLine(stepsText(recipe.steps))
                },
            )
        }
    }

private fun convertRecipeUnitsStrategy() =
    functionalStrategy<String, Unit>("convert-recipe-units") { input ->
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

private fun ingredientsText(ingredients: List<ApiRecipeIngredient>): String =
    ingredients.joinToString("\n") { ingredient ->
        listOf(ingredient.name, ingredient.value, ingredient.unit.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

private fun stepsText(steps: List<String>): String =
    steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")

/**
 * Receives the parts of a recipe whose units the conversion agent's LLM has converted. Each part is
 * reported by its own tool call, so a failure in one part keeps the others.
 */
private class ConvertRecipeMeasurementsTool : ToolSet {
    var ingredients: List<ApiRecipeIngredient>? = null
        private set
    var description: String? = null
        private set
    var steps: List<String>? = null
        private set

    @Tool
    @LLMDescription(
        "Report the recipe's ingredients with their amounts and units converted to the user's preferences.",
    )
    suspend fun reportIngredients(
        @LLMDescription("The ingredients with converted amounts and units.")
        ingredients: List<ApiRecipeIngredient>,
    ): String {
        this.ingredients = ingredients
        return "Saved the converted ingredients."
    }

    @Tool
    @LLMDescription(
        "Report the recipe's description with its units and temperatures converted to the user's preferences.",
    )
    suspend fun reportDescription(
        @LLMDescription("The description with converted units and temperatures.")
        description: String,
    ): String {
        this.description = description
        return "Saved the converted description."
    }

    @Tool
    @LLMDescription(
        "Report the recipe's steps with their units and temperatures converted to the user's preferences.",
    )
    suspend fun reportSteps(
        @LLMDescription("The steps with converted units and temperatures.")
        steps: List<String>,
    ): String {
        this.steps = steps
        return "Saved the converted steps."
    }
}
