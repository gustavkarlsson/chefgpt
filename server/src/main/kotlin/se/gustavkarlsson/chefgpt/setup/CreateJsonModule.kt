package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import kotlinx.serialization.json.Json
import org.koin.dsl.module

fun Application.createJsonModule() =
    module {
        val developmentMode = developmentMode
        single {
            Json {
                encodeDefaults = true
                isLenient = !developmentMode
                explicitNulls = false
                ignoreUnknownKeys = !developmentMode
                allowComments = !developmentMode
                allowTrailingComma = !developmentMode
                prettyPrint = developmentMode
            }
        }
    }
