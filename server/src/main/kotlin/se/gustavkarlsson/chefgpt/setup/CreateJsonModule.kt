package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.chefGptJson

fun Application.createJsonModule() =
    module {
        val developmentMode = developmentMode
        single { chefGptJson(strict = developmentMode, prettyPrint = developmentMode) }
    }
