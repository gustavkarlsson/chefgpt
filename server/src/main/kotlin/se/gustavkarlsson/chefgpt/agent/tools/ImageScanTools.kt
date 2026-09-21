package se.gustavkarlsson.chefgpt.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.agent.describeimages.DescribeImagesAgent
import se.gustavkarlsson.chefgpt.agent.saverecipes.SaveRecipesAgent
import se.gustavkarlsson.chefgpt.agent.scaningredients.ScanIngredientsAgent
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

class DescribePhotosTool(
    private val describeImagesAgent: DescribeImagesAgent,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Describe what the given photos depict, in plain text. The files have been uploaded by the user." +
            " Use this to before deciding what to do with uploaded photos." +
            " Returns one description per file, in the same order as the input.",
    )
    suspend fun describePhotos(
        @LLMDescription("The photo files to describe.")
        files: List<UploadedFile>,
    ): List<String> = describeImagesAgent.run(files)
}

class AddIngredientsFromPhotosTool(
    private val scanIngredientsAgent: ScanIngredientsAgent,
    private val ingredientStore: IngredientStore,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Read the food ingredients in the given photos and add them to the user's inventory." +
            " Returns the added ingredients.",
    )
    suspend fun addIngredientsFromPhotos(
        @LLMDescription("The photo files to scan.")
        files: List<UploadedFile>,
    ): List<String> {
        val scanned = scanIngredientsAgent.scan(userId, files)
        val added = ingredientStore.createIngredients(userId, scanned)
        return added.map { it.name }
    }
}

class AddRecipesFromPhotosTool(
    private val saveRecipesAgent: SaveRecipesAgent,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Read the recipes in the given photos and save them to the user's recipes." +
            " Returns the saved recipe IDs.",
    )
    suspend fun addRecipesFromPhotos(
        @LLMDescription("The photo files to scan.")
        files: List<UploadedFile>,
    ): List<RecipeId> = saveRecipesAgent.scan(userId, files)
}
