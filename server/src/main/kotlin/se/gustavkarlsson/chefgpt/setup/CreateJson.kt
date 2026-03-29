package se.gustavkarlsson.chefgpt.setup

import kotlinx.serialization.json.Json

fun createJson(developmentMode: Boolean): Json =
    Json {
        encodeDefaults = true
        isLenient = !developmentMode
        explicitNulls = false
        ignoreUnknownKeys = !developmentMode
        allowComments = !developmentMode
        allowTrailingComma = !developmentMode
        prettyPrint = developmentMode
    }
