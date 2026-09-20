package se.gustavkarlsson.chefgpt.setup

import ai.koog.ktor.Koog
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import io.ktor.server.application.Application
import io.ktor.server.application.plugin
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.agent.ChatAgent
import se.gustavkarlsson.chefgpt.agent.DescribeImageAgent
import se.gustavkarlsson.chefgpt.agent.FakeChatAgent
import se.gustavkarlsson.chefgpt.agent.FakeDescribeImageAgent
import se.gustavkarlsson.chefgpt.agent.FakeIngredientScanAgent
import se.gustavkarlsson.chefgpt.agent.FakeRecipeScanAgent
import se.gustavkarlsson.chefgpt.agent.IngredientScanAgent
import se.gustavkarlsson.chefgpt.agent.KoogChatAgent
import se.gustavkarlsson.chefgpt.agent.KoogDescribeImageAgent
import se.gustavkarlsson.chefgpt.agent.KoogIngredientScanAgent
import se.gustavkarlsson.chefgpt.agent.KoogRecipeScanAgent
import se.gustavkarlsson.chefgpt.agent.RecipeScanAgent
import se.gustavkarlsson.chefgpt.ai.AiConfig
import se.gustavkarlsson.chefgpt.ai.loadAiConfig
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.files.ImageCropper
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository

private const val CHAT_AGENT = "chat"
private const val INGREDIENT_SCAN_AGENT = "ingredientScan"
private const val RECIPE_SCAN_AGENT = "recipeScan"
private const val DESCRIBE_IMAGE_AGENT = "describeImage"

fun Application.createAiAgentModule() =
    module {
        val config = environment.config
        val aiConfig = config.loadAiConfig()
        single<PromptExecutor> {
            plugin(Koog).promptExecutor
        }
        single {
            when (val type = config.property("bindings.agent").getString()) {
                "llm" -> {
                    val ingredientStore = get<IngredientStore>()
                    val recipeRepository = get<RecipeRepository>()
                    val recipeLookup = get<RecipeLookup>()
                    val imageCropper = get<ImageCropper>()
                    val chatRepository = get<ChatRepository>()
                    val eventRepository = get<EventRepository>()
                    KoogChatAgent(
                        aiConfig.agentModel(CHAT_AGENT),
                        ingredientStore,
                        recipeRepository,
                        recipeLookup,
                        imageCropper,
                        chatRepository,
                        eventRepository,
                        get<RecipeScanAgent>(),
                        get<IngredientScanAgent>(),
                        get<DescribeImageAgent>(),
                    )
                }

                "fake" -> {
                    val eventRepository = get<EventRepository>()
                    FakeChatAgent(eventRepository)
                }

                else -> {
                    error("Unknown agent type: '$type'. Expected 'llm' or 'fake'.")
                }
            }
        } bind ChatAgent::class
        single {
            when (val type = config.property("bindings.agent").getString()) {
                "llm" -> {
                    KoogIngredientScanAgent(
                        get<PromptExecutor>(),
                        aiConfig.agentModel(INGREDIENT_SCAN_AGENT),
                        get<IngredientStore>(),
                    )
                }

                "fake" -> {
                    FakeIngredientScanAgent(get<IngredientStore>())
                }

                else -> {
                    error("Unknown agent type: '$type'. Expected 'llm' or 'fake'.")
                }
            }
        } bind IngredientScanAgent::class
        single {
            when (val type = config.property("bindings.agent").getString()) {
                "llm" -> {
                    KoogRecipeScanAgent(
                        get<PromptExecutor>(),
                        aiConfig.agentModel(RECIPE_SCAN_AGENT),
                        get<RecipeRepository>(),
                        get<RecipeLookup>(),
                        get<ImageCropper>(),
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
                "llm" -> KoogDescribeImageAgent(get<PromptExecutor>(), aiConfig.agentModel(DESCRIBE_IMAGE_AGENT))
                "fake" -> FakeDescribeImageAgent()
                else -> error("Unknown agent type: '$type'. Expected 'llm' or 'fake'.")
            }
        } bind DescribeImageAgent::class
    }

private fun AiConfig.agentModel(agentId: String): LLModel {
    val agentModel = modelsByAgentId[agentId] ?: error("No model configured for agent: '$agentId'")
    require(agentModel.provider in providers) {
        "Agent '$agentId' uses provider '${agentModel.provider.id}' but no API key is configured for it"
    }
    return agentModel.model
}
