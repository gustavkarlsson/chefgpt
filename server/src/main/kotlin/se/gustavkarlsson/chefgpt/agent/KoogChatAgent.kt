package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.ktor.aiAgent
import ai.koog.prompt.llm.LLModel
import io.ktor.server.routing.RoutingContext
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chats.ChatNamingTools
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.facts.toTools
import se.gustavkarlsson.chefgpt.files.FileKind
import se.gustavkarlsson.chefgpt.files.ImageCropper
import se.gustavkarlsson.chefgpt.files.ImageEditTools
import se.gustavkarlsson.chefgpt.files.UploadedFileTools
import se.gustavkarlsson.chefgpt.files.kind
import se.gustavkarlsson.chefgpt.files.sharedAttachments
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.toTools
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.toTools

class KoogChatAgent(
    private val model: LLModel,
    private val ingredientStore: IngredientStore,
    private val recipeRepository: RecipeRepository,
    private val recipeLookup: RecipeLookup,
    private val factRepository: FactRepository,
    private val imageCropper: ImageCropper,
    private val chatRepository: ChatRepository,
    private val eventRepository: EventRepository,
    private val recipeScanAgent: RecipeScanAgent,
    private val ingredientScanAgent: IngredientScanAgent,
    private val describeImageAgent: DescribeImageAgent,
) : ChatAgent {
    override suspend fun RoutingContext.run(
        userId: UserId,
        chatId: ChatId,
    ) {
        // TODO Migrate to the same pattern as the scan agents: build the prompt
        //  manually (system + facts + event history) and run the strategy,
        //  dropping the ktor plugin features and the userId-as-input.
        val agent =
            aiAgent(
                strategy = findRecipeStrategy(),
                model = model,
                tools =
                    ToolRegistry {
                        // Scoped to the user and chat, in addition to globally available tools
                        tools(ingredientStore.toTools(userId))
                        tools(recipeRepository.toTools(userId, recipeLookup))
                        tools(factRepository.toTools(userId))
                        tools(ChatNamingTools(chatRepository, eventRepository, userId, chatId))
                        tools(UploadedFileTools(eventRepository, chatId))
                        tools(
                            ImageEditTools(imageCropper) {
                                eventRepository
                                    .sharedAttachments(chatId)
                                    .filter { it.kind == FileKind.Image }
                                    .map { it.url }
                            },
                        )
                        tools(
                            ImageScanTools(
                                eventRepository,
                                chatId,
                                userId,
                                recipeScanAgent,
                                ingredientScanAgent,
                                describeImageAgent,
                            ),
                        )
                    },
            )
        agent.run(userId, chatId.value.toString())
    }
}
