package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import se.gustavkarlsson.chefgpt.setup.creatEventRepositoryModule
import se.gustavkarlsson.chefgpt.setup.createAiAgentModule
import se.gustavkarlsson.chefgpt.setup.createChatRepositoryModule
import se.gustavkarlsson.chefgpt.setup.createImageUploaderModule
import se.gustavkarlsson.chefgpt.setup.createIngredientStoreModule
import se.gustavkarlsson.chefgpt.setup.createJsonModule
import se.gustavkarlsson.chefgpt.setup.createPostgresModule
import se.gustavkarlsson.chefgpt.setup.createRecipeClientModule
import se.gustavkarlsson.chefgpt.setup.createRethinkDbAccessModule
import se.gustavkarlsson.chefgpt.setup.createSessionStorageModule
import se.gustavkarlsson.chefgpt.setup.createUserRepositoryModule

fun Application.installKoin() {
    val config = environment.config
    install(Koin) {
        slf4jLogger()
        modules(
            createPostgresModule(config),
            createAiAgentModule(config),
            createChatRepositoryModule(config),
            creatEventRepositoryModule(config),
            createImageUploaderModule(config),
            createJsonModule(developmentMode),
            createRethinkDbAccessModule(config),
            createSessionStorageModule(config),
            createRecipeClientModule(config),
            createUserRepositoryModule(config),
            createIngredientStoreModule(config),
        )
        createEagerInstances()
    }
}
