package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.module.requestScope
import se.gustavkarlsson.chefgpt.agent.AiAgent
import se.gustavkarlsson.chefgpt.auth.User
import se.gustavkarlsson.chefgpt.auth.UserRepository
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventRepository
import se.gustavkarlsson.chefgpt.images.ImageUploader
import se.gustavkarlsson.chefgpt.ingredients.IngredientStore
import se.gustavkarlsson.chefgpt.ingredients.PostgresIngredientStore
import se.gustavkarlsson.chefgpt.postgres.PostgresAccess
import se.gustavkarlsson.chefgpt.recipes.RecipeClient
import se.gustavkarlsson.chefgpt.requireSession

fun Application.installDependencies(
    database: PostgresAccess?,
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    eventRepository: EventRepository,
    imageUploader: ImageUploader,
    recipeClient: RecipeClient,
    aiAgent: AiAgent,
    json: Json,
) {
    install(Koin) {
        modules(
            module {
                requestScope {
                    scoped<User> {
                        get<ApplicationCall>().requireSession().user
                    }
                    if (database != null) {
                        scoped<IngredientStore> {
                            PostgresIngredientStore(database, get<User>().id)
                        }
                    }
                }
                single<UserRepository> { userRepository }
                single<ChatRepository> { chatRepository }
                single<EventRepository> { eventRepository }
                single<ImageUploader> { imageUploader }
                single<Json> { json }
                single<RecipeClient> { recipeClient }
                single<AiAgent> { aiAgent }
            },
        )
    }
}
