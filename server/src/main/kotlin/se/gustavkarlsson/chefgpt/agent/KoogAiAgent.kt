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
import se.gustavkarlsson.chefgpt.files.ImageCropper
import se.gustavkarlsson.chefgpt.files.SharedFileTools
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.toTools
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeStore
import se.gustavkarlsson.chefgpt.recipes.toTools

class KoogAiAgent(
    private val model: LLModel,
    private val ingredientStore: IngredientStore,
    private val recipeStore: RecipeStore,
    private val recipeLookup: RecipeLookup,
    private val imageCropper: ImageCropper,
    private val chatRepository: ChatRepository,
    private val eventRepository: EventRepository,
    private val recipeScanAgent: RecipeScanAgent,
    private val ingredientScanAgent: IngredientScanAgent,
    private val describeImageAgent: DescribeImageAgent,
) : AiAgent {
    override suspend fun RoutingContext.run(
        userId: UserId,
        chatId: ChatId,
    ) {
        val agent =
            aiAgent(
                strategy = findRecipeStrategy(),
                model = model,
                tools =
                    ToolRegistry {
                        // Scoped to the user and chat, in addition to globally available tools
                        tools(ingredientStore.toTools(userId))
                        tools(recipeStore.toTools(userId, recipeLookup))
                        tools(ChatNamingTools(chatRepository, eventRepository, userId, chatId))
                        tools(SharedFileTools(eventRepository, imageCropper, chatId))
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
        agent.run(Unit, chatId.value.toString())
    }
}
