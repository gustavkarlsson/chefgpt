package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.files.UploadedFile

@Suppress("unused")
class ImageScanTools(
    private val eventRepository: EventRepository,
    private val chatId: ChatId,
    private val userId: UserId,
    private val recipeScanAgent: RecipeScanAgent,
    private val ingredientScanAgent: IngredientScanAgent,
    private val describeImageAgent: DescribeImageAgent,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Describe what the given photos depict, in plain text. The files have been uploaded by the user." +
            " Use this to before deciding what to do with uploaded photos." +
            " Returns one description per file, in the same order as the input, or null if there was an error.",
    )
    suspend fun describePhotos(
        @LLMDescription("The photo files to describe.")
        files: List<UploadedFile>,
    ): List<String>? = describeImageAgent.scan(userId, files)

    @Tool
    @LLMDescription(
        "Read the food ingredients in the given photos and add them to the user's inventory." +
            " Returns the added ingredients, or null if there was an error.",
    )
    suspend fun scanIngredientsInPhotos(
        @LLMDescription("The photo files to scan.")
        files: List<UploadedFile>,
    ): List<String>? = ingredientScanAgent.scan(userId, files)

    @Tool
    @LLMDescription(
        "Read the recipes in the given photos and save them to the user's recipes." +
            " Returns the saved recipe titles, or null if there was an error.",
    )
    suspend fun scanRecipesInPhotos(
        @LLMDescription("The photo files to scan.")
        files: List<UploadedFile>,
    ): List<String>? = recipeScanAgent.scan(userId, files)
}
