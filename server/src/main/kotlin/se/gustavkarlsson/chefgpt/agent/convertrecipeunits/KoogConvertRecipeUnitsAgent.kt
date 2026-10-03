package se.gustavkarlsson.chefgpt.agent.convertrecipeunits

import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.facts.toMeasurementPromptText
import se.gustavkarlsson.chefgpt.recipes.NewRecipe

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

    Convert only the amounts, units, and temperatures — never change names,
    meaning, order, or any other wording. Preserve formatting exactly, changing
    only the amounts, units, and temperatures.
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
        val converted = convertRecipeUnits(recipe, facts)
        return recipe.copy(
            ingredients = converted.ingredients ?: recipe.ingredients,
            description =
                if (recipe.description == null) {
                    null
                } else {
                    converted.description?.takeIf { it.isNotBlank() } ?: recipe.description
                },
            steps = converted.steps ?: recipe.steps,
        )
    }
}

private class AgenticConvertRecipeUnits(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
) : ConvertRecipeUnits {
    override suspend fun invoke(
        recipe: NewRecipe,
        facts: UserFacts,
    ): ConvertedMeasurements =
        coroutineScope {
            val ingredients = async { convertIngredients(recipe.ingredients, facts) }
            val description =
                recipe.description?.let { description -> async { convertDescription(description, facts) } }
            val steps = async { convertSteps(recipe.steps, facts) }
            ConvertedMeasurements(
                ingredients = ingredients.await(),
                description = description?.await(),
                steps = steps.await(),
            )
        }

    private suspend fun convertIngredients(
        ingredients: List<ApiRecipeIngredient>,
        facts: UserFacts,
    ): List<ApiRecipeIngredient>? =
        if (ingredients.isEmpty()) {
            emptyList()
        } else {
            runStructured("convert-ingredients", ingredientsText(ingredients), facts)
        }

    private suspend fun convertDescription(
        description: String,
        facts: UserFacts,
    ): String? = runStructured("convert-description", description, facts)

    private suspend fun convertSteps(
        steps: List<String>,
        facts: UserFacts,
    ): List<String>? =
        if (steps.isEmpty()) {
            emptyList()
        } else {
            runStructured("convert-steps", stepsText(steps), facts)
        }

    private suspend inline fun <reified T> runStructured(
        name: String,
        content: String,
        facts: UserFacts,
    ): T? =
        promptExecutor
            .executeStructured<T>(
                prompt = buildPrompt(name, content, facts),
                model = model,
            ).fold(
                onSuccess = { it.data },
                onFailure = { null },
            )
}

private fun buildPrompt(
    name: String,
    content: String,
    facts: UserFacts,
): Prompt =
    prompt(name) {
        system(SYSTEM_PROMPT)
        system(facts.toMeasurementPromptText())
        user { text(content) }
    }

private fun ingredientsText(ingredients: List<ApiRecipeIngredient>): String =
    ingredients.joinToString("\n") { ingredient ->
        listOf(ingredient.name, ingredient.value, ingredient.unit.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

private fun stepsText(steps: List<String>): String =
    steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")
