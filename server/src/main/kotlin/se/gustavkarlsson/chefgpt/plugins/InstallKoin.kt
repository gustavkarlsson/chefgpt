package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.core.module.Module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import se.gustavkarlsson.chefgpt.setup.createAiAgentsModule
import se.gustavkarlsson.chefgpt.setup.createChatRepositoryModule
import se.gustavkarlsson.chefgpt.setup.createDatabaseModule
import se.gustavkarlsson.chefgpt.setup.createEventRepositoryModule
import se.gustavkarlsson.chefgpt.setup.createFactRepositoryModule
import se.gustavkarlsson.chefgpt.setup.createFilesModule
import se.gustavkarlsson.chefgpt.setup.createIngredientStoreModule
import se.gustavkarlsson.chefgpt.setup.createJobModule
import se.gustavkarlsson.chefgpt.setup.createJsonModule
import se.gustavkarlsson.chefgpt.setup.createPromptExecutorModule
import se.gustavkarlsson.chefgpt.setup.createRecipeClientModule
import se.gustavkarlsson.chefgpt.setup.createRecipeLookupModule
import se.gustavkarlsson.chefgpt.setup.createRecipeRepositoryModule
import se.gustavkarlsson.chefgpt.setup.createSessionStorageModule
import se.gustavkarlsson.chefgpt.setup.createUserRepositoryModule

fun Application.installKoin(extraKoinModules: List<Module> = emptyList()) {
    install(Koin) {
        slf4jLogger()
        modules(
            createDatabaseModule(),
            createPromptExecutorModule(),
            createAiAgentsModule(),
            createChatRepositoryModule(),
            createEventRepositoryModule(),
            createFilesModule(),
            createJsonModule(),
            createSessionStorageModule(),
            createRecipeClientModule(),
            createRecipeLookupModule(),
            createRecipeRepositoryModule(),
            createUserRepositoryModule(),
            createIngredientStoreModule(),
            createFactRepositoryModule(),
            createJobModule(),
        )
        if (extraKoinModules.isNotEmpty()) {
            modules(extraKoinModules)
        }
        createEagerInstances()
    }
}
