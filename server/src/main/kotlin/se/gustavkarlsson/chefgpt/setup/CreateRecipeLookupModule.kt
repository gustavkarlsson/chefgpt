package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.recipes.RecipeLookup
import se.gustavkarlsson.chefgpt.recipes.RecipeScraper

fun Application.createRecipeLookupModule() =
    module {
        single { RecipeLookup(get(), get()) }
        single { RecipeScraper(get(), get()) }
    }
