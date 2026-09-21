package se.gustavkarlsson.chefgpt.setup

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.agent.FakeRecipeScanAgent
import se.gustavkarlsson.chefgpt.agent.KoogRecipeScanAgent
import se.gustavkarlsson.chefgpt.agent.RecipeScanAgent
import se.gustavkarlsson.chefgpt.agent.chat.ChatAgent
import se.gustavkarlsson.chefgpt.agent.chat.FakeChatAgent
import se.gustavkarlsson.chefgpt.agent.chat.KoogChatAgent
import se.gustavkarlsson.chefgpt.agent.describeimages.DescribeImagesAgent
import se.gustavkarlsson.chefgpt.agent.describeimages.FakeDescribeImagesAgent
import se.gustavkarlsson.chefgpt.agent.describeimages.KoogDescribeImagesAgent
import se.gustavkarlsson.chefgpt.agent.scaningredients.FakeScanIngredientsAgent
import se.gustavkarlsson.chefgpt.agent.scaningredients.KoogScanIngredientsAgent
import se.gustavkarlsson.chefgpt.agent.scaningredients.ScanIngredientsAgent
import se.gustavkarlsson.chefgpt.ai.AiConfig
import se.gustavkarlsson.chefgpt.ai.loadAiConfig
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.files.ImageCropper
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.recipes.RecipeClient
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository

private const val CHAT_AGENT = "chat"
private const val INGREDIENT_SCAN_AGENT = "ingredientScan"
private const val RECIPE_SCAN_AGENT = "recipeScan"
private const val DESCRIBE_IMAGE_AGENT = "describeImage"

fun Application.createAiAgentsModule() =
    module {
        val config = environment.config
        val aiConfig = config.loadAiConfig()
        single {
            when (val type = config.property("bindings.agent").getString()) {
                "llm" -> {
                    KoogChatAgent(
                        promptExecutor = get<PromptExecutor>(),
                        model = aiConfig.agentModel(CHAT_AGENT),
                        ingredientStore = get<IngredientStore>(),
                        recipeRepository = get<RecipeRepository>(),
                        recipeLookup = get<RecipeLookup>(),
                        recipeClient = get<RecipeClient>(),
                        factRepository = get<FactRepository>(),
                        imageCropper = get<ImageCropper>(),
                        chatRepository = get<ChatRepository>(),
                        eventRepository = get<EventRepository>(),
                        recipeScanAgent = get<RecipeScanAgent>(),
                        scanIngredientsAgent = get<ScanIngredientsAgent>(),
                        describeImagesAgent = get<DescribeImagesAgent>(),
                    )
                }

                "fake" -> {
                    FakeChatAgent(get<EventRepository>())
                }

                else -> {
                    error("Unknown agent type: '$type'. Expected 'llm' or 'fake'.")
                }
            }
        } bind ChatAgent::class
        single {
            when (val type = config.property("bindings.agent").getString()) {
                "llm" -> {
                    KoogScanIngredientsAgent(
                        get<PromptExecutor>(),
                        aiConfig.agentModel(INGREDIENT_SCAN_AGENT),
                        get<IngredientStore>(),
                    )
                }

                "fake" -> {
                    FakeScanIngredientsAgent(get<IngredientStore>())
                }

                else -> {
                    error("Unknown agent type: '$type'. Expected 'llm' or 'fake'.")
                }
            }
        } bind ScanIngredientsAgent::class
        single {
            when (val type = config.property("bindings.agent").getString()) {
                "llm" -> {
                    KoogRecipeScanAgent(
                        get<PromptExecutor>(),
                        aiConfig.agentModel(RECIPE_SCAN_AGENT),
                        get<RecipeRepository>(),
                        get<RecipeLookup>(),
                        get<ImageCropper>(),
                        get<FactRepository>(),
                    )
                }

                "fake" -> {
                    FakeRecipeScanAgent(get<RecipeRepository>())
                }

                else -> {
                    error("Unknown agent type: '$type'. Expected 'llm' or 'fake'.")
                }
            }
        } bind RecipeScanAgent::class
        single {
            when (val type = config.property("bindings.agent").getString()) {
                "llm" -> KoogDescribeImagesAgent(get<PromptExecutor>(), aiConfig.agentModel(DESCRIBE_IMAGE_AGENT))
                "fake" -> FakeDescribeImagesAgent()
                else -> error("Unknown agent type: '$type'. Expected 'llm' or 'fake'.")
            }
        } bind DescribeImagesAgent::class
    }

private fun AiConfig.agentModel(agentId: String): LLModel {
    val agentModel = modelsByAgentId[agentId] ?: error("No model configured for agent: '$agentId'")
    require(agentModel.provider in providers) {
        "Agent '$agentId' uses provider '${agentModel.provider.id}' but no API key is configured for it"
    }
    return agentModel.model
}
