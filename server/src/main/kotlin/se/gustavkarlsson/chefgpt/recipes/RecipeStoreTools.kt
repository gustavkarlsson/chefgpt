package se.gustavkarlsson.chefgpt.recipes

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.api.toSummary
import se.gustavkarlsson.chefgpt.auth.UserId
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class RecipeStoreTools(
    private val store: RecipeStore,
    private val lookup: RecipeLookup,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Save a recipe to the user's recipes, so they can look it up later.")
    suspend fun saveRecipe(
        @LLMDescription("The Spoonacular ID of the recipe.")
        spoonacularId: Long,
    ): ApiRecipeSummary {
        val recipe =
            lookup.lookUp(SpoonacularId(spoonacularId))
                ?: error("No recipe found with Spoonacular ID $spoonacularId")
        return store.saveRecipe(userId, recipe).toSummary()
    }

    @Tool
    @LLMDescription(
        "Write a recipe of your own into the user's recipes, for example one you read in a photo " +
            "or a document they shared. Fit what you can read into the fields and leave out what " +
            "is missing — never invent ingredients or steps. Use saveRecipe instead when the " +
            "recipe already exists in Spoonacular.",
    )
    suspend fun createRecipe(
        @LLMDescription("The name of the dish.")
        title: String,
        @LLMDescription("The instructions, one per step.")
        steps: List<String>,
        @LLMDescription("The ingredients, or an empty list if they are unknown.")
        ingredients: List<ApiRecipeIngredient> = emptyList(),
        @LLMDescription("The nutrients, or an empty list if they are unknown.")
        nutrients: List<ApiNutrient> = emptyList(),
        @LLMDescription("A short summary of the dish, or an empty string to leave it out.")
        description: String = "",
        @LLMDescription(
            "The url of a picture to use as the recipe's photo: from listSharedFiles when the " +
                "picture is nothing but the food, and from cropImage when it also holds writing " +
                "or background. Empty string for no photo.",
        )
        imageUrl: String = "",
        @LLMDescription("The preparation time in minutes, or 0 if it is unknown.")
        preparationMinutes: Int = 0,
        @LLMDescription("The cooking time in minutes, or 0 if it is unknown.")
        cookingMinutes: Int = 0,
        @LLMDescription("The total time in minutes, or 0 if it is unknown.")
        totalMinutes: Int = 0,
    ): ApiRecipeSummary {
        require(title.isNotBlank()) { "A recipe needs a title" }
        require(steps.isNotEmpty()) { "A recipe needs at least one step" }
        val recipe =
            NewRecipe(
                title = title,
                steps = steps,
                imageUrl = recipePhotoUrlOrNull(imageUrl),
                description = description.ifBlank { null },
                preparationDuration = preparationMinutes.minutesOrNull(),
                cookingDuration = cookingMinutes.minutesOrNull(),
                duration = totalMinutes.minutesOrNull(),
                ingredients = ingredients,
                nutrients = nutrients,
            )
        return store.saveRecipe(userId, recipe).toSummary()
    }

    @Tool
    @LLMDescription("Get all the user's recipes, without their instructions and ingredients.")
    suspend fun listRecipes(): List<ApiRecipeSummary> = store.getRecipeSummaries(userId)

    @Tool
    @LLMDescription(
        "Mark one of the user's recipes as a favorite, meaning one they intend to come back to, " +
            "or no longer a favorite.",
    )
    suspend fun setRecipeFavorite(
        @LLMDescription("The ID of the recipe.")
        recipeId: String,
        @LLMDescription("True to make the recipe a favorite, false to make it an ordinary saved recipe.")
        favorite: Boolean,
    ): ApiRecipe =
        store.setFavorite(userId, recipeId.toRecipeId(), favorite)
            ?: error("No recipe found with ID $recipeId")

    @Tool
    @LLMDescription("Get one of the user's recipes in full, including instructions, ingredients and nutrients.")
    suspend fun getRecipe(
        @LLMDescription("The ID of the recipe.")
        recipeId: String,
    ): ApiRecipe = store.getRecipe(userId, recipeId.toRecipeId()) ?: error("No recipe found with ID $recipeId")

    @Tool
    @LLMDescription(
        "Rewrite parts of one of the user's recipes, for example to substitute an ingredient or change servings. " +
            "Read the recipe first, and only pass the parts that should change — " +
            "anything left out keeps its current value. Lists replace the current list entirely. " +
            "The rewrite is stored as a modified version with its own ID, which takes the recipe's place " +
            "for the user until they keep it as a copy or let it overwrite the original.",
    )
    suspend fun modifyRecipe(
        @LLMDescription("The ID of the recipe to rewrite.")
        recipeId: String,
        @LLMDescription("The new title, or an empty string to keep the current one.")
        title: String = "",
        @LLMDescription("The new description, or an empty string to keep the current one.")
        description: String = "",
        @LLMDescription("The new preparation time in minutes, or 0 to keep the current one.")
        preparationMinutes: Int = 0,
        @LLMDescription("The new cooking time in minutes, or 0 to keep the current one.")
        cookingMinutes: Int = 0,
        @LLMDescription("The new total time in minutes, or 0 to keep the current one.")
        totalMinutes: Int = 0,
        @LLMDescription("The new instructions, one per step, or an empty list to keep the current ones.")
        steps: List<String> = emptyList(),
        @LLMDescription("The new ingredients, or an empty list to keep the current ones.")
        ingredients: List<ApiRecipeIngredient> = emptyList(),
        @LLMDescription("The new nutrients, or an empty list to keep the current ones.")
        nutrients: List<ApiNutrient> = emptyList(),
    ): ApiRecipe {
        val update =
            RecipeUpdate(
                title = title.ifEmpty { null },
                description = description.ifEmpty { null },
                preparationDuration = preparationMinutes.minutesOrNull(),
                cookingDuration = cookingMinutes.minutesOrNull(),
                duration = totalMinutes.minutesOrNull(),
                steps = steps.ifEmpty { null },
                ingredients = ingredients.ifEmpty { null },
                nutrients = nutrients.ifEmpty { null },
            )
        return store.modifyRecipe(userId, recipeId.toRecipeId(), update)
            ?: error("No recipe found with ID $recipeId")
    }

    @Tool
    @LLMDescription(
        "Let a modified recipe replace the recipe it was modified from, deleting that one.",
    )
    suspend fun overwriteOriginalRecipe(
        @LLMDescription("The ID of the modified recipe.")
        recipeId: String,
    ): ApiRecipe =
        store.overwriteOriginal(userId, recipeId.toRecipeId())
            ?: error("No modified recipe found with ID $recipeId")

    @Tool
    @LLMDescription(
        "Keep a modified recipe alongside the recipe it was modified from, so the user has both.",
    )
    suspend fun saveRecipeAsCopy(
        @LLMDescription("The ID of the modified recipe.")
        recipeId: String,
    ): ApiRecipe =
        store.saveAsCopy(userId, recipeId.toRecipeId())
            ?: error("No modified recipe found with ID $recipeId")
}

fun RecipeStore.toTools(
    userId: UserId,
    lookup: RecipeLookup,
): ToolSet = RecipeStoreTools(this, lookup, userId)

private fun String.toRecipeId(): RecipeId = RecipeId.parseOrNull(this) ?: error("Invalid recipe ID: $this")

private fun Int.minutesOrNull() = takeIf { it > 0 }?.minutes
