package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup

fun Application.createRecipeLookupModule() =
    module {
        single { RecipeLookup(get(), get()) }
    }
