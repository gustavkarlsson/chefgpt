package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.github.michaelbull.result.fold
import se.gustavkarlsson.chefgpt.api.ApiAttachment
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.files.AttachmentKind
import se.gustavkarlsson.chefgpt.files.kind
import se.gustavkarlsson.chefgpt.files.sharedAttachments

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
        "Read the recipes in the given photos and save them to the user's recipes. The urls come " +
            "from listSharedFiles. Returns what was saved.",
    )
    suspend fun scanRecipesInPhotos(
        @LLMDescription("The urls of the photos to scan, from listSharedFiles.")
        urls: List<String>,
    ): String =
        recipeScanAgent.scan(userId, resolveImages(urls)).fold(
            { summaries -> formatSavedRecipes(summaries) },
            { reason -> "Could not scan recipes: $reason" },
        )

    @Tool
    @LLMDescription(
        "Read the food ingredients in the given photos and add them to the user's inventory. The " +
            "urls come from listSharedFiles. Returns how many ingredients were found.",
    )
    suspend fun scanIngredientsInPhotos(
        @LLMDescription("The urls of the photos to scan, from listSharedFiles.")
        urls: List<String>,
    ): String =
        ingredientScanAgent.scan(userId, resolveImages(urls)).fold(
            { count -> "Found $count ingredient(s)." },
            { reason -> "Could not scan ingredients: $reason" },
        )

    @Tool
    @LLMDescription(
        "Describe what the given photos show, in plain text. The urls come from listSharedFiles. " +
            "Use this to learn what a photo shows before deciding what to do with it.",
    )
    suspend fun describePhotos(
        @LLMDescription("The urls of the photos to describe, from listSharedFiles.")
        urls: List<String>,
    ): String =
        describeImageAgent.scan(userId, resolveImages(urls)).fold(
            { description -> description },
            { reason -> "Could not describe photos: $reason" },
        )

    private suspend fun resolveImages(urls: List<String>): List<ApiAttachment> {
        val shared = eventRepository.sharedAttachments(chatId)
        return urls.map { url ->
            requireNotNull(shared.firstOrNull { it.url == url && it.kind == AttachmentKind.Image }) {
                "No picture shared in this chat has the url $url"
            }
        }
    }
}

private fun formatSavedRecipes(summaries: List<ApiRecipeSummary>): String =
    when (summaries.size) {
        0 -> "Saved 0 recipes."
        1 -> "Saved 1 recipe: ${summaries.title}"
        else -> "Saved ${summaries.size} recipe(s): ${summaries.joinToString(", ") { it.title }}"
    }
