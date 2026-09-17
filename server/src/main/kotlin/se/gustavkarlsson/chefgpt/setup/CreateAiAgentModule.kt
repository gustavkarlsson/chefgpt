package se.gustavkarlsson.chefgpt.setup

import ai.koog.prompt.llm.LLModel
import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.agent.AiAgent
import se.gustavkarlsson.chefgpt.agent.FakeAiAgent
import se.gustavkarlsson.chefgpt.agent.FakeIngredientScanAgent
import se.gustavkarlsson.chefgpt.agent.IngredientScanAgent
import se.gustavkarlsson.chefgpt.agent.KoogAiAgent
import se.gustavkarlsson.chefgpt.agent.KoogIngredientScanAgent
import se.gustavkarlsson.chefgpt.ai.AiConfig
import se.gustavkarlsson.chefgpt.ai.loadAiConfig
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.files.ImageCropper
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeStore

private const val CHAT_AGENT = "chat"
private const val INGREDIENT_SCAN_AGENT = "ingredientScan"

fun Application.createAiAgentModule() =
    module {
        val config = environment.config
        val aiConfig = config.loadAiConfig()
        single {
            when (val type = config.property("bindings.agent").getString()) {
                "llm" -> {
                    val ingredientStore = get<IngredientStore>()
                    val recipeStore = get<RecipeStore>()
                    val recipeLookup = get<RecipeLookup>()
                    val imageCropper = get<ImageCropper>()
                    val chatRepository = get<ChatRepository>()
                    val eventRepository = get<EventRepository>()
                    KoogAiAgent(
                        aiConfig.agentModel(CHAT_AGENT),
                        ingredientStore,
                        recipeStore,
                        recipeLookup,
                        imageCropper,
                        chatRepository,
                        eventRepository,
                    )
                }

                "fake" -> {
                    val eventRepository = get<EventRepository>()
                    FakeAiAgent(eventRepository)
                }

                else -> {
                    error("Unknown agent type: '$type'. Expected 'llm' or 'fake'.")
                }
            }
        } bind AiAgent::class
        single {
            when (val type = config.property("bindings.agent").getString()) {
                "llm" -> KoogIngredientScanAgent(aiConfig.agentModel(INGREDIENT_SCAN_AGENT), get<IngredientStore>())
                "fake" -> FakeIngredientScanAgent(get<IngredientStore>())
                else -> error("Unknown agent type: '$type'. Expected 'llm' or 'fake'.")
            }
        } bind IngredientScanAgent::class
    }

private fun AiConfig.agentModel(agentId: String): LLModel {
    val agentModel = modelsByAgentId[agentId] ?: error("No model configured for agent: '$agentId'")
    require(agentModel.provider in providers) {
        "Agent '$agentId' uses provider '${agentModel.provider.id}' but no API key is configured for it"
    }
    return agentModel.model
}
