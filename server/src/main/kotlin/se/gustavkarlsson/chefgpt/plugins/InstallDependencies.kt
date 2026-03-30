package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import kotlinx.serialization.json.Json
import se.gustavkarlsson.chefgpt.agent.AiAgent
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.images.ImageUploader
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess
import se.gustavkarlsson.chefgpt.recipes.SpoonacularClient

fun Application.installDependencies(
    database: PostgresAccess?,
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    eventRepository: EventRepository,
    imageUploader: ImageUploader,
    spoonacularClient: SpoonacularClient,
    aiAgent: AiAgent,
    json: Json,
) {
    dependencies {
        if (database != null) {
            provide { database }
        }
        provide<UserRepository> { userRepository }
        provide<ChatRepository> { chatRepository }
        provide<EventRepository> { eventRepository }
        provide { imageUploader }
        provide { json }
        provide { spoonacularClient }
        provide<AiAgent> { aiAgent }
    }
}
