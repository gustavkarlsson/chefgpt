package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.agent.AiAgent
import se.gustavkarlsson.chefgpt.agent.FakeAiAgent
import se.gustavkarlsson.chefgpt.agent.FakeIngredientScanAgent
import se.gustavkarlsson.chefgpt.agent.IngredientScanAgent
import se.gustavkarlsson.chefgpt.agent.KoogAiAgent
import se.gustavkarlsson.chefgpt.agent.KoogIngredientScanAgent
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore

fun Application.createAiAgentModule() =
    module {
        val config = environment.config
        single {
            when (val type = config.property("bindings.agent").getString()) {
                "llm" -> {
                    val ingredientStore = get<IngredientStore>()
                    val chatRepository = get<ChatRepository>()
                    val eventRepository = get<EventRepository>()
                    KoogAiAgent(ingredientStore, chatRepository, eventRepository)
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
                "llm" -> KoogIngredientScanAgent(get<IngredientStore>())
                "fake" -> FakeIngredientScanAgent(get<IngredientStore>())
                else -> error("Unknown agent type: '$type'. Expected 'llm' or 'fake'.")
            }
        } bind IngredientScanAgent::class
    }
