package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.module.requestScope
import se.gustavkarlsson.chefgpt.agent.AiAgent
import se.gustavkarlsson.chefgpt.agent.FakeAiAgent
import se.gustavkarlsson.chefgpt.agent.KoogAiAgent
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.recipes.RecipeClient

fun createAiAgentModule(config: ApplicationConfig) =
    module {
        requestScope {
            scoped {
                when (val type = config.property("chefgpt.agent").getString()) {
                    "llm" -> {
                        val recipeClient = get<RecipeClient>()
                        val ingredientStore = get<IngredientStore>()
                        KoogAiAgent(recipeClient, ingredientStore)
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
        }
    }
