package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.agent.describeimages.DescribeImagesAgent
import se.gustavkarlsson.chefgpt.agent.scaningredients.ScanIngredientsAgent
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

@Suppress("unused")
class ImageScanTools(
    private val eventRepository: EventRepository,
    private val chatId: ChatId,
    private val userId: UserId,
    private val recipeScanAgent: RecipeScanAgent,
    private val scanIngredientsAgent: ScanIngredientsAgent,
    private val describeImagesAgent: DescribeImagesAgent,
    private val ingredientStore: IngredientStore,
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

    @Tool
    @LLMDescription(
        "Read the food ingredients in the given photos and add them to the user's inventory." +
            " Returns the added ingredients.",
    )
    suspend fun scanIngredientsInPhotos(
        @LLMDescription("The photo files to scan.")
        files: List<UploadedFile>,
    ): List<String> {
        val scanned = scanIngredientsAgent.scan(userId, files)
        val added = ingredientStore.createIngredients(userId, scanned)
        return added.map { it.name }
    }

    @Tool
    @LLMDescription(
        "Read the recipes in the given photos and save them to the user's recipes." +
            " Returns the saved recipe titles.",
    )
    suspend fun scanRecipesInPhotos(
        @LLMDescription("The photo files to scan.")
        files: List<UploadedFile>,
    ): List<String> = recipeScanAgent.scan(userId, files)
}
