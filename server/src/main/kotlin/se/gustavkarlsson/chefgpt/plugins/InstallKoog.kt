package se.gustavkarlsson.chefgpt.plugins

import ai.koog.ktor.Koog
import io.ktor.server.application.Application
import io.ktor.server.application.install
import se.gustavkarlsson.chefgpt.agent.EventBackedChatMemory
import se.gustavkarlsson.chefgpt.chats.EventRepository

fun Application.installKoog(
    anthropicApiKey: String,
    eventRepository: EventRepository,
) {
    install(Koog) {
        llm {
            anthropic(apiKey = anthropicApiKey)
        }
        agentConfig {
            prompt {
                system(
                    """
                    You are a culinary expert specialized finding the perfect recipe.
                    based on the user's ingredients, time of day, and mood.

                    Start by greeting the user. If they have not added any ingredients,
                    suggest that they do so using text or by taking a photo.

                    When there are ingredients, ask the user what they would like to cook,
                    and give a subtle hint based on the time of day.

                    Use the recipe tools together with the ingredient store
                    and other context to suggest some recipes.
                    Present each recipe found with a super short description and URL.

                    If there are too few results, suggest that the user updates their ingredients.

                    When the used has picked a recipe, send the recipe to them.
                    """.trimIndent(),
                )
            }
            install(EventBackedChatMemory) {
                this.eventRepository = eventRepository
            }
        }
    }
}
