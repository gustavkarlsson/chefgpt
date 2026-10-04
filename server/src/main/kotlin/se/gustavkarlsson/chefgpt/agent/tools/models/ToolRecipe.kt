package se.gustavkarlsson.chefgpt.agent.tools.models

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.recipes.Nutrient
import se.gustavkarlsson.chefgpt.recipes.Recipe
import se.gustavkarlsson.chefgpt.recipes.RecipeIngredient
import se.gustavkarlsson.chefgpt.recipes.RecipeSummary

@Serializable
@LLMDescription("An ingredient in a recipe, with its amount and unit.")
data class ToolRecipeIngredient(
    @property:LLMDescription("The name of the ingredient, e.g. 'olive oil'.")
    val name: String,
    @property:LLMDescription("The amount, e.g. '2' or '2.5'.")
    val value: String,
    @property:LLMDescription("The unit, e.g. 'tbsp' or 'g', or null when the amount has no unit.")
    val unit: String?,
)

@Serializable
@LLMDescription("A nutrient in a recipe, with its amount and unit.")
data class ToolNutrient(
    @property:LLMDescription("The name of the nutrient, e.g. 'Calories'.")
    val name: String,
    @property:LLMDescription("The amount, e.g. '450' or '12.5'.")
    val value: String,
    @property:LLMDescription("The unit, e.g. 'kcal' or 'g', or null when the amount has no unit.")
    val unit: String?,
)

@Serializable
@LLMDescription("A saved recipe's list entry, without its instructions, ingredients and nutrients.")
data class ToolRecipeSummary(
    @property:LLMDescription("The ID of the recipe.")
    val id: String,
    @property:LLMDescription("The name of the dish.")
    val title: String,
    @property:LLMDescription("Whether the recipe is a favorite.")
    val favorite: Boolean,
    @property:LLMDescription(
        "The ID of the recipe this one was modified from, or null if it is not a modification.",
    )
    val modifiedFrom: String?,
    @property:LLMDescription("The Spoonacular ID of the recipe, or null if it was not saved from Spoonacular.")
    val spoonacularId: Long?,
    @property:LLMDescription("The url of the recipe's photo, or null if there is none.")
    val imageUrl: String?,
)

@Serializable
@LLMDescription("A saved recipe, including its instructions, ingredients and nutrients.")
data class ToolRecipe(
    @property:LLMDescription("The ID of the recipe.")
    val id: String,
    @property:LLMDescription("The name of the dish.")
    val title: String,
    @property:LLMDescription("The instructions, one per step.")
    val steps: List<String>,
    @property:LLMDescription("The ingredients.")
    val ingredients: List<ToolRecipeIngredient>,
    @property:LLMDescription("The nutrients.")
    val nutrients: List<ToolNutrient>,
    @property:LLMDescription("A short summary of the dish, or null if there is none.")
    val description: String?,
    @property:LLMDescription("The url of the recipe's photo, or null if there is none.")
    val imageUrl: String?,
    @property:LLMDescription("The Spoonacular ID of the recipe, or null if it was not saved from Spoonacular.")
    val spoonacularId: Long?,
    @property:LLMDescription("Whether the recipe is a favorite.")
    val favorite: Boolean,
    @property:LLMDescription(
        "The ID of the recipe this one was modified from, or null if it is not a modification.",
    )
    val modifiedFrom: String?,
    @property:LLMDescription("The preparation time in minutes, or null if it is unknown.")
    val preparationMinutes: Int?,
    @property:LLMDescription("The cooking time in minutes, or null if it is unknown.")
    val cookingMinutes: Int?,
    @property:LLMDescription("The total time in minutes, or null if it is unknown.")
    val totalMinutes: Int?,
    @property:LLMDescription(
        "The number of servings the recipe makes: an exact count like \"4\", or a range like \"4-6\".",
    )
    val servings: String?,
)

fun Recipe.toTool(): ToolRecipe =
    ToolRecipe(
        id = id.value.toString(),
        title = title,
        steps = steps,
        ingredients = ingredients.map { it.toTool() },
        nutrients = nutrients.map { it.toTool() },
        description = description,
        imageUrl = imageUrl?.value,
        spoonacularId = spoonacularId?.value,
        favorite = favorite,
        modifiedFrom = modifiedFrom?.value?.toString(),
        preparationMinutes = preparationDuration?.inWholeMinutes?.toInt(),
        cookingMinutes = cookingDuration?.inWholeMinutes?.toInt(),
        totalMinutes = duration?.inWholeMinutes?.toInt(),
        servings = servings?.toToolServings(),
    )

fun RecipeSummary.toTool(): ToolRecipeSummary =
    ToolRecipeSummary(
        id = id.value.toString(),
        title = title,
        favorite = favorite,
        modifiedFrom = modifiedFrom?.value?.toString(),
        spoonacularId = spoonacularId?.value,
        imageUrl = imageUrl?.value,
    )

fun RecipeIngredient.toTool(): ToolRecipeIngredient = ToolRecipeIngredient(name, value, unit)

fun Nutrient.toTool(): ToolNutrient = ToolNutrient(name, value, unit)

fun ToolRecipeIngredient.toDomain(): RecipeIngredient = RecipeIngredient(name, value, unit)

fun ToolNutrient.toDomain(): Nutrient = Nutrient(name, value, unit)

private fun IntRange.toToolServings(): String = if (first == last) "$first" else "$first-$last"
