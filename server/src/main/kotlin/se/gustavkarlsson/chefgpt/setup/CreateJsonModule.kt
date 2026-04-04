package se.gustavkarlsson.chefgpt.setup

import kotlinx.serialization.json.Json
import org.koin.dsl.module

fun createJsonModule(developmentMode: Boolean) =
    module {
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
