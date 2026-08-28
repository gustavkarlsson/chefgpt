package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.core.module.Module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import se.gustavkarlsson.chefgpt.setup.createAiAgentModule
import se.gustavkarlsson.chefgpt.setup.createChatRepositoryModule
import se.gustavkarlsson.chefgpt.setup.createDatabaseModule
import se.gustavkarlsson.chefgpt.setup.createEventRepositoryModule
import se.gustavkarlsson.chefgpt.setup.createFilesModule
import se.gustavkarlsson.chefgpt.setup.createIngredientStoreModule
import se.gustavkarlsson.chefgpt.setup.createJsonModule
import se.gustavkarlsson.chefgpt.setup.createRecipeClientModule
import se.gustavkarlsson.chefgpt.setup.createRecipeLookupModule
import se.gustavkarlsson.chefgpt.setup.createRecipeStoreModule
import se.gustavkarlsson.chefgpt.setup.createSessionStorageModule
import se.gustavkarlsson.chefgpt.setup.createUserRepositoryModule

fun Application.installKoin(extraKoinModules: List<Module> = emptyList()) {
    install(Koin) {
        slf4jLogger()
        modules(
            createDatabaseModule(),
            createAiAgentModule(),
            createChatRepositoryModule(),
            createEventRepositoryModule(),
            createFilesModule(),
            createJsonModule(),
            createSessionStorageModule(),
            createRecipeClientModule(),
            createRecipeLookupModule(),
            createRecipeStoreModule(),
            createUserRepositoryModule(),
            createIngredientStoreModule(),
        )
        if (extraKoinModules.isNotEmpty()) {
            modules(extraKoinModules)
        }
        createEagerInstances()
    }
}
