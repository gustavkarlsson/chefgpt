package se.gustavkarlsson.chefgpt.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.get

fun Application.installContentNegotiation() {
    val json = get<Json>()
    install(ContentNegotiation) {
        json(json)
    }
}
